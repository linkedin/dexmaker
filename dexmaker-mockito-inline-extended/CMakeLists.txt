cmake_minimum_required(VERSION 3.4.1)

set(slicer_sources
    ../dexmaker-mockito-inline/external/slicer/bytecode_encoder.cc
    ../dexmaker-mockito-inline/external/slicer/code_ir.cc
    ../dexmaker-mockito-inline/external/slicer/common.cc
    ../dexmaker-mockito-inline/external/slicer/control_flow_graph.cc
    ../dexmaker-mockito-inline/external/slicer/debuginfo_encoder.cc
    ../dexmaker-mockito-inline/external/slicer/dex_bytecode.cc
    ../dexmaker-mockito-inline/external/slicer/dex_format.cc
    ../dexmaker-mockito-inline/external/slicer/dex_ir_builder.cc
    ../dexmaker-mockito-inline/external/slicer/dex_ir.cc
    ../dexmaker-mockito-inline/external/slicer/dex_utf8.cc
    ../dexmaker-mockito-inline/external/slicer/instrumentation.cc
    ../dexmaker-mockito-inline/external/slicer/reader.cc
    ../dexmaker-mockito-inline/external/slicer/tryblocks_encoder.cc
    ../dexmaker-mockito-inline/external/slicer/writer.cc)

add_library(slicer
            STATIC
            ${slicer_sources})

include_directories(../dexmaker-mockito-inline/external/jdk ../dexmaker-mockito-inline/external/slicer/export/)

target_link_libraries(slicer z)

add_library(staticjvmtiagent
            SHARED
            src/main/jni/staticjvmtiagent/agent.cc)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -DANDROID_STL=c++_shared -frtti -Wall -Werror -Wno-unused-parameter -Wno-shift-count-overflow -Wno-error=non-virtual-dtor -Wno-sign-compare -Wno-switch -Wno-missing-braces")

target_link_libraries(staticjvmtiagent slicer)
