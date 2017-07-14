task taskdecl {
  Int i = 3
  command {}
  output {
    Int o = 4
  }
}

task taskdecl_optional {
  Int? i = 3
  command {
    echo "Surprisingly, this needs a different command to disambiguate from taskdecl above."
  }
  output {
    Int o = 4
  }

}

workflow declaration_override_caching {
  call taskdecl
  # should not cache to the above
  call taskdecl as taskdecl_mismatched { input: i = taskdecl.o }
  # should cache to the above
  call taskdecl as taskdecl_matched { input: i = taskdecl.o - 1 }

  call taskdecl_optional
  # should not cache to the above
  call taskdecl_optional as taskdecl_optional_mismatched { input: i = taskdecl_optional.o }
  # should cache to the above
  call taskdecl_optional as taskdecl_optional_matched { input: i = taskdecl_optional.o - 1 }
}


