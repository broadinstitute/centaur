task failing_task {
    command {
        # C'est nul !
        exit 1
    }
    output {
        Boolean done = true
    }
}

workflow dontCacheToFailedJobs {
  call failing_task
}
