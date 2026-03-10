cmake_minimum_required(VERSION 3.4.1)
enable_language(C)
enable_language(CXX)

find_library(LIBLOG log)
find_library(LIBZ z)

set(WABBITEMU_COMNFLAGS "-Wno-unused-command-line-argument -funroll-loops -fsigned-char -ffast-math -fno-signed-zeros -ggdb")
add_definitions(-DHIGH_SHADE_GIF -DVERBOSE -D_ANDROID)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ${WABBITEMU_COMNFLAGS} -std=c11")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ${WABBITEMU_COMNFLAGS} -felide-constructors -std=c++14")

set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Ofast -flto")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Ofast -flto")

set(WABBITEMU_SRC
    core/core.c
    core/indexcb.c
    core/alu.c
    core/device.c
    core/control.c
    interface/calc.c
    interface/state.c
    hardware/81hw.c
    hardware/83hw.c
    hardware/83phw.c
    hardware/83psehw.c
    hardware/86hw.c
    hardware/lcd.c
    hardware/link.c
    hardware/colorlcd.c
    hardware/keys.c
    utilities/var.c
    utilities/savestate.cpp
    utilities/sendfile.c
    utilities/label.c
    utilities/gif.cpp
    utilities/linksendvar.c
    utilities/screenshothandle.c
    utilities/exportvar.c
    utilities/sound.c
    utilities/breakpoint.c
    utilities/zpipe.c
    wabbitemujni.c)

add_library(Wabbitemu SHARED ${WABBITEMU_SRC})
target_link_options(Wabbitemu PRIVATE "-Wl,-z,max-page-size=16384")
target_link_libraries(Wabbitemu ${LIBLOG} ${LIBZ})
target_include_directories(Wabbitemu PRIVATE . core utilities interface hardware)
