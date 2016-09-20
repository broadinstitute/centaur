task hello {
  File addressee
  command {
    echo "Hello ${read_string(addressee)}!"
  }
  output {
    String salutation = read_string(stdout())
  }
  runtime {
    docker: "ubuntu:latest"
  }
}

workflow hello {
  call hello
  output {
     hello.salutation
  }
}
