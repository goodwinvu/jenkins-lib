package ru.pulsar.jenkins.library.configuration

import com.cloudbees.groovy.cps.NonCPS
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import ru.pulsar.jenkins.library.IStepExecutor
import ru.pulsar.jenkins.library.ioc.ContextRegistry

@JsonIgnoreProperties(ignoreUnknown = true)
class JobConfiguration implements Serializable {
    @JsonPropertyDescription("Версия платформы 1С:Предприятие в формате 8.3.хх.хххх.")
    String v8version

    @JsonPropertyDescription("Версия модуля 1C:Enterprise Development Tools формате xxxx.x.x:x86_64")
    String edtVersion

    @JsonPropertyDescription("Путь к корневому каталогу с исходниками конфигурации, в случае хранения исходников в формате EDT, необходимо указать путь к проекту")
    String srcDir

    @JsonPropertyDescription("Формат исходников конфигурации")
    @JsonProperty(defaultValue = "designer")
    SourceFormat sourceFormat

    @JsonProperty("stages")
    @JsonPropertyDescription("Включение этапов сборок")
    StageFlags stageFlags

    @JsonProperty("timeout")
    @JsonPropertyDescription("Настройка таймаутов для шагов")
    TimeoutOptions timeoutOptions

    @JsonPropertyDescription("Имя ветки по умолчанию. Значение по умолчанию - main.")
    String defaultBranch

    @JsonPropertyDescription("Идентификаторы сохраненных секретов")
    Secrets secrets

    @JsonPropertyDescription("Сохранение конфигурации ИБ (cf и cft) в артефакты сборки")
    Boolean saveCFtoArtifacts
    
    @JsonPropertyDescription("Имя GitLab сервера в настройках Jenkins, для передачи статусов сборок.")
    String gitlabInstanceName
    
    @JsonPropertyDescription("Названия сборок, которые будут иметь доступ к артефактам с помощью плагина Copy Artifact. По умолчанию, разрешено для всех - *")
    String permissionArtifacts
    
    @JsonProperty("initInfobase")
    @JsonPropertyDescription("Настройки шага инициализации ИБ")
    InitInfoBaseOptions initInfoBaseOptions

    @JsonProperty("bdd")
    @JsonPropertyDescription("Настройки шага запуска BDD сценариев")
    BddOptions bddOptions

    @JsonProperty("sonarqube")
    @JsonPropertyDescription("Настройки анализа SonarQube")
    SonarQubeOptions sonarQubeOptions

    @JsonProperty("syntaxCheck")
    @JsonPropertyDescription("Настройки синтаксического контроля")
    SyntaxCheckOptions syntaxCheckOptions

    @JsonProperty("smoke")
    @JsonPropertyDescription("Настройки дымового тестирования")
    SmokeTestOptions smokeTestOptions

    @JsonProperty("yaxunit")
    @JsonPropertyDescription("Настройки YAXUnit")
    YaxunitOptions yaxunitOptions

    @JsonProperty("resultsTransform")
    @JsonPropertyDescription("Настройки трансформации результатов анализа")
    ResultsTransformOptions resultsTransformOptions

    @JsonProperty("notifications")
    @JsonPropertyDescription("Настройки рассылки результатов сборки")
    NotificationsOptions notificationsOptions

    @JsonProperty("logosConfig")
    @JsonPropertyDescription("Конфигурация библиотеки logos. Применяется перед запуском каждой стадии сборки")
    String logosConfig

    @Override
    @NonCPS
    String toString() {
        return "JobConfiguration{" +
            "v8version='" + v8version + '\'' +
            ", edtVersion='" + edtVersion + '\'' +
            ", srcDir='" + srcDir + '\'' +
            ", sourceFormat=" + sourceFormat +
            ", stageFlags=" + stageFlags +
            ", timeoutOptions=" + timeoutOptions +
            ", defaultBranch='" + defaultBranch + '\'' +
            ", secrets=" + secrets +
            ", saveCFtoArtifacts='" + saveCFtoArtifacts + '\'' +
            ", gitlabInstanceName='" + gitlabInstanceName + '\'' +
            ", permissionArtifacts='" + permissionArtifacts + '\'' +
            ", initInfoBaseOptions=" + initInfoBaseOptions +
            ", bddOptions=" + bddOptions +
            ", sonarQubeOptions=" + sonarQubeOptions +
            ", syntaxCheckOptions=" + syntaxCheckOptions +
            ", smokeTestOptions=" + smokeTestOptions +
            ", yaxunitOptions=" + yaxunitOptions +
            ", resultsTransformOptions=" + resultsTransformOptions +
            ", notificationOptions=" + notificationsOptions +
            ", logosConfig='" + logosConfig + '\'' +
            '}'
    }

    boolean infoBaseFromFiles() {
        IStepExecutor steps = ContextRegistry.getContext().getStepExecutor()
        def env = steps.env()
        String branchName = env.BRANCH_NAME
        def initMethod = initInfoBaseOptions.initMethod

        return (initMethod == InitInfoBaseMethod.FROM_SOURCE) ||
            (initMethod == InitInfoBaseMethod.DEFAULT_BRANCH_FROM_STORAGE && branchName != defaultBranch)
    }

    boolean needLoadExtensions(String stageName = "") {
        if (stageName.isEmpty()) {
            return initInfoBaseOptions.extensions.length != 0
        } else {
            return initInfoBaseOptions.extensions.any { extension ->
                extension.stages.contains(stageName)
            }
        }
    }

    boolean templateDBLoaded() {
        return initInfoBaseOptions.templateDBPath != null && !initInfoBaseOptions.templateDBPath.isEmpty()
    }

    String v8AgentLabel() {
        return v8version
    }

    String edtAgentLabel() {
        String edtVersionForRing = "edt"
        if (edtVersion != '') {
            edtVersionForRing += "@" + edtVersion
        }
        return edtVersionForRing
    }
}