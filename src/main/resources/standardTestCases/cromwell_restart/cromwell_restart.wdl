task cromwell_killer {
    command {
        # runs long enough to give time to Centaur to see that it's Running and shutdown Cromwell
        sleep 60
        echo "Arrrrgggggggg !" > out
        chmod go+w out
    }
    output {
        File out = "out"
    }
    runtime {
        docker: "ubuntu:latest"
    }
}

workflow cromwell_restart {
    call cromwell_killer
}
