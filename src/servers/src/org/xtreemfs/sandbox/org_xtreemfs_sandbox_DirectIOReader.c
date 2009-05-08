#include "org_xtreemfs_sandbox_DirectIOReader.h"

#include <jni.h>
#include <sys/types.h>
#include <sys/ipc.h>
#include <sys/shm.h>
#include <sys/mman.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <unistd.h>
#include <stdlib.h>

JNIEXPORT jobject JNICALL Java_org_xtreemfs_sandbox_DirectIOReader_loadFile
  (JNIEnv *env, jobject jobj, jstring name)

{
    void* m;
    jobject jb;
    jboolean iscopy;
    struct stat finfo;
    const char *mfile = (*env)->GetStringUTFChars(
                env, name, &iscopy);
    int fd = open(mfile, O_RDONLY | 040000);
    if(!fd)
       printf("could not open file");
    
    lstat(mfile, &finfo);
    m = valloc(finfo.st_size);
    
    int c;        
    c = read(fd, m, finfo.st_size);
    if(c != finfo.st_size)
       printf("read wrong object size");

    jb=(*env)->NewDirectByteBuffer(env, m, finfo.st_size);
    close(fd);
    (*env)->ReleaseStringUTFChars(env, name, mfile);
    return (jb);

}
