task quay {
    command {
        echo "hello"
    }
    runtime {
        docker: "quay.io/tjeandet/tj-ubuntu:centaur"
    }
}

workflow docker_hash_quay {
    call quay 
}
