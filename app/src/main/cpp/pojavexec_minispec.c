//
// Created by maks on 30.06.2026.
//

#include <stdbool.h>
#include <dlfcn.h>
#include <jni.h>

typedef void* (*acquire_egl_handle_t)(const char*);

typedef struct {
    acquire_egl_handle_t egl_acquire;
    const char* egl_path;
    int force_gles_context;
    int override_major_version;
    bool force_recreate_on_resize;
    int disp_width;
    int disp_height;
    int disp_hz;
} pojavexec_renderspec_t;

void* acq_dlopen_wrapper(const char* name) {
    return dlopen(name, RTLD_NOW);
}

pojavexec_renderspec_t spec = {
        .egl_acquire = acq_dlopen_wrapper,
        .egl_path = "libltw.so",
        .force_gles_context = 1,
        .override_major_version = 3
};

JNIEXPORT void JNICALL
Java_git_artdeell_dnbootstrap_MainActivity_updateDisplayProperties(JNIEnv *env, jclass clazz, jint width, jint height, jint hz) {
    spec.disp_width = width;
    spec.disp_height = height;
    spec.disp_hz = hz;
}

__attribute((used, visibility("default")))
const pojavexec_renderspec_t *pojavexec_getRenderSpec() {
    return &spec;
}

__attribute((used, visibility("default")))
const void *pojavexec_loadVulkanDriver() {
    return NULL;
}
