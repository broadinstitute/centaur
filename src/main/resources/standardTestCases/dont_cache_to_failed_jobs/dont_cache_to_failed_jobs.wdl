task failing_task {
    command {
        # C'est nul !
        exit 1
    }
    runtime {
        docker: "ubuntu@sha256:71cd81252a3563a03ad8daee81047b62ab5d892ebbfbf71cf53415f29c130950"
    }
    output {
        Boolean done = true
    }
}

workflow dont_cache_to_failed_jobs {
  call failing_task
}
