task reverse {
  String var = "racecar"
  command {
   echo "$var | rev"
  }
  output {
    String done = read_string(stdout())
  }
  runtime {
    docker: "ubuntu:latest"
  }
}

workflow refresh_token_no_auth_bucket {
  call reverse
  output {
     reverse.done
  }
}