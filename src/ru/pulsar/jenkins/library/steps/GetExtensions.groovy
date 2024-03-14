package ru.pulsar.jenkins.library.steps

import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.configuration.JobConfiguration
import ru.pulsar.jenkins.library.configuration.SourceFormat
import ru.pulsar.jenkins.library.configuration.InitExtensionMethod
import ru.pulsar.jenkins.library.configuration.InitInfoBaseOptions.Extension
import ru.pulsar.jenkins.library.ioc.ContextRegistry
import ru.pulsar.jenkins.library.utils.Logger
import ru.pulsar.jenkins.library.utils.VRunner
import hudson.FilePath
import ru.pulsar.jenkins.library.utils.FileUtils

class GetExtensions implements Serializable {

    public static final String EXTENSIONS_OUT_DIR = 'build/out/cfe'

    private final JobConfiguration config;

    GetExtensions(JobConfiguration config) {
        this.config = config
    }

    def run() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()

        Logger.printLocation()

        def env = steps.env()

        steps.installLocalDependencies();

        String vrunnerPath = initVRunnerPath();

        Logger.println("Сборка расширений")

        String sourceDirName = ""
        if (config.sourceFormat == SourceFormat.EDT) {
            sourceDirName = "$env.WORKSPACE/$EdtToDesignerFormatTransformation.EXTENSION_DIR"
        } else {
            sourceDirName = "$env.WORKSPACE"
        }
        extractConvertedExtensions(sourceDirName, steps)

        config.initInfoBaseOptions.extensions.each {
            if (it.initMethod == InitExtensionMethod.SOURCE) {
                Logger.println("Сборка расширения ${it.name} из исходников")
                String srcDir = getSrcDir(it, sourceDirName)
                buildExtension(it, srcDir, vrunnerPath, steps)
            } elseif (it.initMethod == InitExtensionMethod.FILE){
                Logger.println("Загрузка расширения ${it.name} из интернета по ссылке ${it.path}")
                loadExtension(it, env)
            } else {
                Logger.println("Неизвестный метод инициализации расширения ${it.name}")
            }
        }
    }

    private void buildExtension(Extension extension, String srcDir, String vrunnerPath, IStepExecutor steps) {
        // ТУТ ГРЯЗНЫЙ ХАК
        VRunner.exec("mkdir -p $EXTENSIONS_OUT_DIR")
        // ТУТ КОНЕЦ ГРЯЗНОГО ХАКА

        def compileExtCommand = "$vrunnerPath compileexttocfe --src ${srcDir} --out $EXTENSIONS_OUT_DIR/${extension.name}.cfe"
        List<String> logosConfig = ["LOGOS_CONFIG=$config.logosConfig"]
        steps.withEnv(logosConfig) {
            VRunner.exec(compileExtCommand)
        }
    }

    private void loadExtension(Extension extension, def env) {
        String pathToExtension = "$env.WORKSPACE/${EXTENSIONS_OUT_DIR}/${extension.name}.cfe"
        FilePath localPathToExtension = FileUtils.getFilePath(pathToExtension)

        if (extension.path.startsWith("http")) {
            // If the path is a URL, download the file
            localPathToExtension.copyFrom(new URL(extension.path))
        } else {
            // If the path is a local file, copy the file
            FilePath localFilePath = FileUtils.getFilePath(extension.path)
            localPathToExtension.copyFrom(localFilePath.toURI().toURL())
        }
    }


    private String initVRunnerPath() {
        return VRunner.getVRunnerPath()
    }

    private String getSrcDir(Extension extension, String sourceDirName) {
        if (config.sourceFormat == SourceFormat.EDT) {
            return "${sourceDirName}/${extension.name}"
        } else {
            return "${sourceDirName}/${extension.path}"
        }
    }

    private void extractConvertedExtensions(String sourceDirName, IStepExecutor steps) {
        if (config.sourceFormat == SourceFormat.EDT) {
            // распакуем расширения
            steps.unstash(EdtToDesignerFormatTransformation.EXTENSION_ZIP_STASH)
            steps.unzip(sourceDirName, EdtToDesignerFormatTransformation.EXTENSION_ZIP)
        }
    }
}
