task find_wf_logs {
#out should be 1
    command {
    cd gs://cloud-cromwell-dev/centaur-tests/direct_logs
    ls -l | grep -c .log
    }
    output {
     String out = read_string(stdout())
    }
    runtime {
    docker: "ubuntu:latest"
    }
}

task clear_wf_logs {
  String x
    command {
    cd gs://cloud-cromwell-dev/centaur-tests/direct_logs
    rm *.log
    ls -l | grep -c .log
    }
    output {
     String out = read_string(stdout())
    }
    runtime {
    docker: "ubuntu:latest"
    }
}

workflow direct_logs {
  #call createDir
  call find_wf_logs
  call clear_wf_logs { input: x = find_wf_logs.out }
  output {
    find_wf_logs.out
  }
}
