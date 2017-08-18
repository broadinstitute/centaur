task hello {
  command {
    echo "Hello " > test.out
    exit 1
  }
  output {
    File out = "test.out"
  }
  runtime {
    docker: "ubuntu:latest"
  }
}

workflow final_call_logs_dir_fail {
  call hello
  output {
     hello.out
  }
}
