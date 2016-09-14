task makeFilesTask {

    command <<<
        touch one.unique.txt
        touch two.unique.txt

        mkdir Reads
        touch ./Reads/three.unique.txt
        touch ./Reads/four.unique.txt
    >>>
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Array[File] glob1 = glob("./Reads/*.unique.txt")
        Array[File] glob2 = glob("*.unique.txt")
    }

}

task checkGlobTask {
    Array[File] file_glob1
    Array[File] file_glob2

    command <<<
        echo ${sep=' ' file_glob1} | wc  -w > glob1.txt
        echo ${sep=' ' file_glob2} | wc -w > glob2.txt
    >>>
    runtime {
        docker: "ubuntu:latest"
    }
    output {
        Int out_txt1 = read_int("glob1.txt")
        Int out_txt2 = read_int("glob2.txt")
    }
}

workflow accurateglobbing {

    call makeFilesTask {}

    call checkGlobTask {
        input:
        file_glob1=makeFilesTask.glob1,
        file_glob2=makeFilesTask.glob2
    }
    output {
            checkGlobTask.*
        }
}
