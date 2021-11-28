package ru.pulsar.jenkins.library.steps


import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Constants
import ru.pulsar.jenkins.library.utils.Logger

class EdtToDesignerFormatTransformation implements Serializable {

    public static final String EXT_PATH_PEFIX = 'build'
    public static final String EXT_PATH_SUFFIX = 'ext_'
    public static final String WORKSPACE = 'build/edt-workspace'
    public static final String CONFIGURATION_DIR = 'build/cfg'
    public static final String CONFIGURATION_ZIP = 'build/cfg.zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'
    public static final String CONFIGURATION_ZIP_STASH = 'cfg-zip'

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
 
        def srcDir = config.srcDir
        def srcExtDir = config.srcExtDir
        def projectDir = new File("$env.WORKSPACE/$srcDir").getCanonicalPath()
        def workspaceDir = "$env.WORKSPACE/$WORKSPACE" 
        def configurationRoot = "$env.WORKSPACE/$CONFIGURATION_DIR"
        def configurationZip = "$CONFIGURATION_ZIP"

        steps.deleteDir(workspaceDir)
        steps.deleteDir(configurationRoot)
        
        def extPrefix = "$EXT_PATH_PEFIX"
        def extSuffix = "$EXT_PATH_SUFFIX"

        Logger.println("Конвертация исходников из формата EDT в формат Конфигуратора")

        def ringCommand = "ring edt workspace export --workspace-location \"$workspaceDir\" --project \"$projectDir\" --configuration-files \"$configurationRoot\""
        
        def ringOpts = [Constants.DEFAULT_RING_OPTS]
        steps.withEnv(ringOpts) {
            steps.cmd(ringCommand)

            srcExtDir.each{
                
                def workspaceExtDir = workspaceDir.replace(extPrefix,"$extPrefix/$extSuffix${it}")
                def projectExtDir = new File("$env.WORKSPACE/${it}").getCanonicalPath()
                def configurationExtRoot = configurationRoot.replace(extPrefix,"$extPrefix/$extSuffix${it}") 
                def configurationExtZip = configurationZip.replace(extPrefix,"$extPrefix/$extSuffix${it}")

                def ringCommandExt = "ring edt workspace export --workspace-location \"$workspaceExtDir\" --project \"$projectExtDir\" --configuration-files \"$configurationExtRoot\""
                
                steps.deleteDir(workspaceExtDir)
                steps.deleteDir(configurationExtRoot)

                Logger.println("Конвертация исходников расширения ${it} из формата EDT в формат Конфигуратора")                
                steps.cmd(ringCommandExt)
                
                steps.zip(configurationExtRoot, configurationExtZip)
                steps.stash("${it}_$CONFIGURATION_ZIP_STASH", configurationExtZip)
            }  
        }
        
        steps.zip(CONFIGURATION_DIR, CONFIGURATION_ZIP)
        steps.stash(CONFIGURATION_ZIP_STASH, CONFIGURATION_ZIP)
    }

}
