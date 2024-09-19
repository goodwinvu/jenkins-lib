package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.configuration.InitExtensionMethod
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.EDT
import ru.pulsar.jenkins.library.utils.FileUtils
import ru.pulsar.jenkins.library.utils.Logger

class EdtToDesignerFormatTransformation implements Serializable {

    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String CONFIGURATION_DIR = 'build/cfg'
    public static final String CONFIGURATION_ZIP = 'build/cfg.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'
    public static final String EXTENSION_DIR = 'build/cfe_src'
    public static final String EXTENSION_ZIP = 'build/cfe_src.zip'
    public static final String EXTENSION_ZIP_STASH = 'cfe_src-zip'

    private final JobConfiguration config;

    EdtToDesignerFormatTransformation(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        if (config.sourceFormat != SourceFormat.EDT) {
            Logger.println("SRC is not in EDT format. No transform is needed.")
            return
        }

        def env = steps.env();

        String srcDir = config.srcDir
        String workspaceDir = FileUtils.getFilePath("$env.WORKSPACE/$WORKSPACE").getRemote()

        String projectWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cf").getRemote()
        String projectDir = FileUtils.getFilePath("$env.WORKSPACE/$srcDir").getRemote()
        String configurationRoot = FileUtils.getFilePath("$env.WORKSPACE/$CONFIGURATION_DIR").getRemote()


        String extensionRoot = FileUtils.getFilePath("$env.WORKSPACE/$EXTENSION_DIR").getRemote()
        steps.deleteDir(workspaceDir)
        def edtVersionForRing = EDT.ringModule(config)
        def useCli = EDT.useEDTCli(config)
        def pathEDT = EDT.getEDTPath(config)

        transformConfiguration(steps, projectDir, projectWorkspaceDir, configurationRoot, edtVersionForRing, useCli, pathEDT)
        transformExtensions(steps, workspaceDir, extensionRoot, edtVersionForRing, useCli, pathEDT)
 
    }

    private void transformConfiguration(IStepExecutor steps, String projectDir, String projectWorkspaceDir, String configurationRoot, String edtVersionForRing, Boolean useCli, String pathEDT) {
        String edtCommand = ''
        Logger.println("Конвертация исходников конфигурации из формата EDT в формат Конфигуратора")
        steps.deleteDir(configurationRoot)
        if (useCli) {       
            edtCommand = "$pathEDT/1cedtcli -data \"$projectWorkspaceDir\" -timeout 5400 -vmargs -Xmx8g -command export --project \"$projectDir\" --configuration-files \"$configurationRoot\""
        }else{
            edtCommand = "ring $edtVersionForRing workspace export --workspace-location \"$projectWorkspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""
        }
        steps.ringCommand(edtCommand)
        steps.zip(CONFIGURATION_DIR, CONFIGURATION_ZIP)
        steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)
    }

    private void transformExtensions(IStepExecutor steps, String workspaceDir, String extensionRoot, String edtVersionForRing, Boolean useCli, String pathEDT) {
        String edtCommand = ''
        steps.deleteDir(extensionRoot)

        config.initInfoBaseOptions.extensions.each {
            if (it.initMethod != InitExtensionMethod.SOURCE) {
                return
            }
            Logger.println("Конвертация исходников расширения ${it.name} из формата EDT в формат Конфигуратора")
            def env = steps.env();
            def projectDir = FileUtils.getFilePath("$env.WORKSPACE/${it.path}")
            def currentExtensionWorkspaceDir = FileUtils.getFilePath("$workspaceDir/cfe/${it.name}")
            if (useCli) {      
                edtCommand = "$pathEDT/1cedtcli -data \"$currentExtensionWorkspaceDir\" -vmargs -Xmx8g -command export --project \"$projectDir\" --configuration-files \"$extensionRoot/${it.name}\""
            }else{
                edtCommand = "ring $edtVersionForRing workspace export --workspace-location \"$currentExtensionWorkspaceDir\" --project \"$projectDir\" --configuration-files \"$extensionRoot/${it.name}\""
            }
            
            steps.ringCommand(edtCommand)
        }
        steps.zip(EXTENSION_DIR, EXTENSION_ZIP)
        steps.stash(EXTENSION_ZIP_STASH, EXTENSION_ZIP)
    }

}
