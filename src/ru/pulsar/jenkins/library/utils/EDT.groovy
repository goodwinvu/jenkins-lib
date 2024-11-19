package ru.pulsar.jenkins.library.utils

import ru.pulsar.jenkins.library.configuration.JobConfiguration

final class EDT {

    private final JobConfiguration config

    EDT(JobConfiguration config) {
        this.config = config 
    }

    static String ringModule(JobConfiguration config) {
        return config.edtAgentLabel()
    }

}