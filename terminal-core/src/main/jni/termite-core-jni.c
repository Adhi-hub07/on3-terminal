#include <stddef.h>
#include <stdint.h>

#define JNI_TRUE 1
#define JNI_FALSE 0
#define JNI_FSIZE sizeof(void*)
#define JNI_OK 0

typedef int jint;
typedef signed char jbyte;
typedef unsigned char jboolean;
typedef short jshort;
typedef long long jlong;
typedef float jfloat;
typedef double jdouble;
typedef jint jsize;
typedef unsigned short jchar;

typedef struct _jobject {}* jobject;
typedef jobject jclass;
typedef jobject jstring;
typedef jobject jarray;
typedef jobject jobjectArray;
typedef jobject jbooleanArray;
typedef jobject jbyteArray;
typedef jobject jcharArray;
typedef jobject jshortArray;
typedef jobject jintArray;
typedef jobject jlongArray;
typedef jobject jfloatArray;
typedef jobject jdoubleArray;
typedef jobject jthrowable;
typedef jobject jweak;

typedef struct {
    char* str;
    int len;
    int capacity;
} JNIString;

typedef union jvalue {
    jboolean z;
    jbyte b;
    jchar c;
    jshort s;
    jint i;
    jlong j;
    jfloat f;
    jdouble d;
    jobject l;
} jvalue;

typedef struct JNINativeInterface_ {
    void* reserved0;
    void* reserved1;
    void* reserved2;
    void* reserved3;
    jint (*GetVersion)(void*);
    jclass (*DefineClass)(void*, const char*, jobject, const jbyte*, jsize);
    jclass (*FindClass)(void*, const char*);
    void* (*FromReflectedMethod)(void*, jobject);
    void* (*FromReflectedField)(void*, jobject);
    void* (*ToReflectedMethod)(void*, jclass, void*, jboolean);
    jclass (*GetSuperclass)(void*, jclass);
    jboolean (*IsAssignableFrom)(void*, jclass, jclass);
    void* (*ToReflectedField)(void*, jclass, void*, jboolean);
    jint (*Throw)(void*, jthrowable);
    jint (*ThrowNew)(void*, jclass, const char*);
    jthrowable (*ExceptionOccurred)(void*);
    void (*ExceptionDescribe)(void*);
    void (*ExceptionClear)(void*);
    void (*FatalError)(void*, const char*);
    void* reserved4;
    jint (*PushLocalFrame)(void*, jint);
    jboolean (*PopLocalFrame)(void*, jobject);
    jobject (*NewGlobalRef)(void*, jobject);
    void (*DeleteGlobalRef)(void*, jobject);
    void (*DeleteLocalRef)(void*, jobject);
    jboolean (*IsSameObject)(void*, jobject, jobject);
    void* (*NewLocalRef)(void*, jobject);
    jint (*EnsureLocalCapacity)(void*, jint);
    jobject (*AllocObject)(void*, jclass);
    jobject (*NewObject)(void*, jclass, void*, ...);
    void* (*NewObjectV)(void*, jclass, void*, void*);
    void* (*NewObjectA)(void*, jclass, void*, jvalue*);
    jclass (*GetObjectClass)(void*, jobject);
    jboolean (*IsInstanceOf)(void*, jobject, jclass);
    void* (*GetMethodID)(void*, jclass, const char*, const char*);
    void* (*CallObjectMethod)(void*, jobject, void*, ...);
    void* (*CallObjectMethodV)(void*, jobject, void*, void*);
    void* (*CallObjectMethodA)(void*, jobject, void*, jvalue*);
    jboolean (*CallBooleanMethod)(void*, jobject, void*, ...);
    void* (*CallBooleanMethodV)(void*, jobject, void*, void*);
    void* (*CallBooleanMethodA)(void*, jobject, void*, jvalue*);
    jbyte (*CallByteMethod)(void*, jobject, void*, ...);
    void* (*CallByteMethodV)(void*, jobject, void*, void*);
    void* (*CallByteMethodA)(void*, jobject, void*, jvalue*);
    jshort (*CallShortMethod)(void*, jobject, void*, ...);
    void* (*CallShortMethodV)(void*, jobject, void*, void*);
    void* (*CallShortMethodA)(void*, jobject, void*, jvalue*);
    jint (*CallIntMethod)(void*, jobject, void*, ...);
    void* (*CallIntMethodV)(void*, jobject, void*, void*);
    void* (*CallIntMethodA)(void*, jobject, void*, jvalue*);
    jlong (*CallLongMethod)(void*, jobject, void*, ...);
    void* (*CallLongMethodV)(void*, jobject, void*, void*);
    void* (*CallLongMethodA)(void*, jobject, void*, jvalue*);
    jfloat (*CallFloatMethod)(void*, jobject, void*, ...);
    void* (*CallFloatMethodV)(void*, jobject, void*, void*);
    void* (*CallFloatMethodA)(void*, jobject, void*, jvalue*);
    jdouble (*CallDoubleMethod)(void*, jobject, void*, ...);
    void* (*CallDoubleMethodV)(void*, jobject, void*, void*);
    void* (*CallDoubleMethodA)(void*, jobject, void*, jvalue*);
    void (*CallVoidMethod)(void*, jobject, void*, ...);
    void* (*CallVoidMethodV)(void*, jobject, void*, void*);
    void* (*CallVoidMethodA)(void*, jobject, void*, jvalue*);
    void* (*GetStaticMethodID)(void*, jclass, const char*, const char*);
    void* (*CallStaticObjectMethod)(void*, jclass, void*, ...);
    void* (*CallStaticObjectMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticObjectMethodA)(void*, jclass, void*, jvalue*);
    jboolean (*CallStaticBooleanMethod)(void*, jclass, void*, ...);
    void* (*CallStaticBooleanMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticBooleanMethodA)(void*, jclass, void*, jvalue*);
    jbyte (*CallStaticByteMethod)(void*, jclass, void*, ...);
    void* (*CallStaticByteMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticByteMethodA)(void*, jclass, void*, jvalue*);
    jshort (*CallStaticShortMethod)(void*, jclass, void*, ...);
    void* (*CallStaticShortMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticShortMethodA)(void*, jclass, void*, jvalue*);
    jint (*CallStaticIntMethod)(void*, jclass, void*, ...);
    void* (*CallStaticIntMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticIntMethodA)(void*, jclass, void*, jvalue*);
    jlong (*CallStaticLongMethod)(void*, jclass, void*, ...);
    void* (*CallStaticLongMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticLongMethodA)(void*, jclass, void*, jvalue*);
    jfloat (*CallStaticFloatMethod)(void*, jclass, void*, ...);
    void* (*CallStaticFloatMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticFloatMethodA)(void*, jclass, void*, jvalue*);
    jdouble (*CallStaticDoubleMethod)(void*, jclass, void*, ...);
    void* (*CallStaticDoubleMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticDoubleMethodA)(void*, jclass, void*, jvalue*);
    void (*CallStaticVoidMethod)(void*, jclass, void*, ...);
    void* (*CallStaticVoidMethodV)(void*, jclass, void*, void*);
    void* (*CallStaticVoidMethodA)(void*, jclass, void*, jvalue*);
    jstring (*NewString)(void*, const jchar*, jsize);
    jsize (*GetStringLength)(void*, jstring);
    const jchar* (*GetStringChars)(void*, jstring, jboolean*);
    void (*ReleaseStringChars)(void*, jstring, const jchar*);
    jstring (*NewStringUTF)(void*, const char*);
    jsize (*GetStringUTFLength)(void*, jstring);
    const char* (*GetStringUTFChars)(void*, jstring, jboolean*);
    void (*ReleaseStringUTFChars)(void*, jstring, const char*);
    jsize (*GetArrayLength)(void*, jarray);
    jobjectArray (*NewObjectArray)(void*, jsize, jclass, jobject);
    jobject (*GetObjectArrayElement)(void*, jobjectArray, jsize);
    void (*SetObjectArrayElement)(void*, jobjectArray, jsize, jobject);
    jbooleanArray (*NewBooleanArray)(void*, jsize);
    jbyteArray (*NewByteArray)(void*, jsize);
    jcharArray (*NewCharArray)(void*, jsize);
    jshortArray (*NewShortArray)(void*, jsize);
    jintArray (*NewIntArray)(void*, jsize);
    jlongArray (*NewLongArray)(void*, jsize);
    jfloatArray (*NewFloatArray)(void*, jsize);
    jdoubleArray (*NewDoubleArray)(void*, jsize);
    jboolean* (*GetBooleanArrayElements)(void*, jbooleanArray, jboolean*);
    jbyte* (*GetByteArrayElements)(void*, jbyteArray, jboolean*);
    jchar* (*GetCharArrayElements)(void*, jcharArray, jboolean*);
    jshort* (*GetShortArrayElements)(void*, jshortArray, jboolean*);
    jint* (*GetIntArrayElements)(void*, jintArray, jboolean*);
    jlong* (*GetLongArrayElements)(void*, jlongArray, jboolean*);
    jfloat* (*GetFloatArrayElements)(void*, jfloatArray, jboolean*);
    jdouble* (*GetDoubleArrayElements)(void*, jdoubleArray, jboolean*);
    void (*ReleaseBooleanArrayElements)(void*, jbooleanArray, jboolean*, jint);
    void (*ReleaseByteArrayElements)(void*, jbyteArray, jbyte*, jint);
    void (*ReleaseCharArrayElements)(void*, jcharArray, jchar*, jint);
    void (*ReleaseShortArrayElements)(void*, jshortArray, jshort*, jint);
    void (*ReleaseIntArrayElements)(void*, jintArray, jint*, jint);
    void (*ReleaseLongArrayElements)(void*, jlongArray, jlong*, jint);
    void (*ReleaseFloatArrayElements)(void*, jfloatArray, jfloat*, jint);
    void (*ReleaseDoubleArrayElements)(void*, jdoubleArray, jdouble*, jint);
    void (*GetBooleanArrayRegion)(void*, jbooleanArray, jsize, jsize, jboolean*);
    void (*GetByteArrayRegion)(void*, jbyteArray, jsize, jsize, jbyte*);
    void (*GetCharArrayRegion)(void*, jcharArray, jsize, jsize, jchar*);
    void (*GetShortArrayRegion)(void*, jshortArray, jsize, jsize, jshort*);
    void (*GetIntArrayRegion)(void*, jintArray, jsize, jsize, jint*);
    void (*GetLongArrayRegion)(void*, jlongArray, jsize, jsize, jlong*);
    void (*GetFloatArrayRegion)(void*, jfloatArray, jsize, jsize, jfloat*);
    void (*GetDoubleArrayRegion)(void*, jdoubleArray, jsize, jsize, jdouble*);
    void (*SetBooleanArrayRegion)(void*, jbooleanArray, jsize, jsize, const jboolean*);
    void (*SetByteArrayRegion)(void*, jbyteArray, jsize, jsize, const jbyte*);
    void (*SetCharArrayRegion)(void*, jcharArray, jsize, jsize, const jchar*);
    void (*SetShortArrayRegion)(void*, jshortArray, jsize, jsize, const jshort*);
    void (*SetIntArrayRegion)(void*, jintArray, jsize, jsize, const jint*);
    void (*SetLongArrayRegion)(void*, jlongArray, jsize, jsize, const jlong*);
    void (*SetFloatArrayRegion)(void*, jfloatArray, jsize, jsize, const jfloat*);
    void (*SetDoubleArrayRegion)(void*, jdoubleArray, jsize, jsize, const jdouble*);
    jint (*RegisterNatives)(void*, jclass, const void*, jint);
    jint (*UnregisterNatives)(void*, jclass);
    jint (*GetJavaVM)(void*, void**);
    void (*GetStringRegion)(void*, jstring, jsize, jsize, jchar*);
    void (*GetStringUTFRegion)(void*, jstring, jsize, jsize, char*);
    void* (*GetPrimitiveArrayCritical)(void*, jarray, jboolean*);
    void (*ReleasePrimitiveArrayCritical)(void*, jarray, void*, jint);
    const jchar* (*GetStringCritical)(void*, jstring, jboolean*);
    void (*ReleaseStringCritical)(void*, jstring, const jchar*);
    jweak (*NewWeakGlobalRef)(void*, jobject);
    void (*DeleteWeakGlobalRef)(void*, jweak);
    jboolean (*ExceptionCheck)(void*);
    void* reserved5;
    void* reserved6;
    void* reserved7;
    void* reserved8;
    void* reserved9;
    void* reserved10;
    void* reserved11;
    void* reserved12;
    void* reserved13;
    void* reserved14;
    void* reserved15;
    void* reserved16;
    void* reserved17;
    void* reserved18;
    void* reserved19;
    void* reserved20;
    void* reserved21;
    void* reserved22;
    void* reserved23;
    void* reserved24;
    void* reserved25;
    void* reserved26;
    void* reserved27;
    void* reserved28;
    void* reserved29;
    void* reserved30;
    void* reserved31;
    void* reserved32;
    void* reserved33;
    void* reserved34;
    void* reserved35;
    void* reserved36;
    void* reserved37;
    void* reserved38;
    void* reserved39;
    void* reserved40;
    void* reserved41;
    void* reserved42;
    void* reserved43;
    void* reserved44;
    void* reserved45;
    void* reserved46;
    void* reserved47;
    void* reserved48;
    void* reserved49;
    void* reserved50;
    void* reserved51;
    void* reserved52;
    void* reserved53;
    void* reserved54;
    void* reserved55;
    void* reserved56;
    void* reserved57;
    void* reserved58;
    void* reserved59;
    void* reserved60;
    void* reserved61;
    void* reserved62;
    void* reserved63;
    void* reserved64;
    void* reserved65;
    void* reserved66;
    void* reserved67;
    void* reserved68;
    void* reserved69;
    void* reserved70;
    void* reserved71;
    void* reserved72;
    void* reserved73;
    void* reserved74;
    void* reserved75;
    void* reserved76;
    void* reserved77;
    void* reserved78;
    void* reserved79;
    void* reserved80;
    void* reserved81;
    void* reserved82;
    void* reserved83;
    void* reserved84;
    void* reserved85;
    void* reserved86;
    void* reserved87;
    void* reserved88;
    void* reserved89;
    void* reserved90;
    void* reserved91;
    void* reserved92;
    void* reserved93;
    void* reserved94;
    void* reserved95;
    void* reserved96;
    void* reserved97;
    void* reserved98;
    void* reserved99;
    void* reserved100;
} JNINativeInterface;

typedef const JNINativeInterface* JNIEnv;

#define JNIEXPORT __attribute__((visibility("default")))
#define JNICALL

typedef struct {
    jint version;
    void** functions;
} JavaVM;

typedef struct {
    unsigned short ws_row;
    unsigned short ws_col;
    unsigned short ws_xpixel;
    unsigned short ws_ypixel;
} winsize_t;

#define AT_FDCWD (-100)
#define PROT_READ 1
#define PROT_WRITE 2
#define MAP_PRIVATE 2
#define MAP_ANONYMOUS 0x20
#define O_RDWR 2
#define O_CLOEXEC 0x80000
#define SIGCHLD 17
#define SIG_DFL ((void*)0)
#define TIOCGPTN 0x80045430
#define TIOCSPTLCK 0x40045431
#define TIOCSWINSZ 0x40085467
#define EINTR 4

static long syscall6(long n, long a1, long a2, long a3, long a4, long a5, long a6) {
    register long x8 asm("x8") = n;
    register long x0 asm("x0") = a1;
    register long x1 asm("x1") = a2;
    register long x2 asm("x2") = a3;
    register long x3 asm("x3") = a4;
    register long x4 asm("x4") = a5;
    register long x5 asm("x5") = a6;
    asm volatile("svc #0" : "+r"(x0) : "r"(x8), "r"(x1), "r"(x2), "r"(x3), "r"(x4), "r"(x5) : "memory");
    return x0;
}

static long syscall5(long n, long a1, long a2, long a3, long a4, long a5) {
    return syscall6(n, a1, a2, a3, a4, a5, 0);
}

static long syscall4(long n, long a1, long a2, long a3, long a4) {
    return syscall6(n, a1, a2, a3, a4, 0, 0);
}

static long syscall3(long n, long a1, long a2, long a3) {
    return syscall6(n, a1, a2, a3, 0, 0, 0);
}

static long syscall2(long n, long a1, long a2) {
    return syscall6(n, a1, a2, 0, 0, 0, 0);
}

static long syscall1(long n, long a1) {
    return syscall6(n, a1, 0, 0, 0, 0, 0);
}

static long mmap_alloc(void* addr, long length, int prot, int flags, int fd, long offset) {
    return syscall6(222, (long)addr, length, prot, flags, fd, offset);
}

static long mmap_free(void* addr, long length) {
    return syscall2(215, (long)addr, length);
}

static long open_ptmx(void) {
    return syscall3(56, AT_FDCWD, (long)"/dev/ptmx", O_RDWR | O_CLOEXEC);
}

static long open_ptmx2(void) {
    return syscall3(56, AT_FDCWD, (long)"/dev/pts/ptmx", O_RDWR | O_CLOEXEC);
}

static long close_fd(int fd) {
    return syscall1(57, fd);
}

static long ioctl_fd(int fd, long request, long arg) {
    return syscall3(29, fd, request, arg);
}

static long read_fd(int fd, void* buf, long count) {
    return syscall3(63, fd, (long)buf, count);
}

static long write_fd(int fd, const void* buf, long count) {
    return syscall3(64, fd, (long)buf, count);
}

static long fork_proc(void) {
    return syscall2(220, SIGCHLD, 0);
}

static long execve_proc(const char* path, char* const argv[], char* const envp[]) {
    return syscall3(221, (long)path, (long)argv, (long)envp);
}

static long wait4_pid(int pid, int* status, int options) {
    return syscall4(260, pid, (long)status, options, 0);
}

static long setsid_proc(void) {
    return syscall1(112, 0);
}

static long dup3_fd(int oldfd, int newfd, int flags) {
    return syscall3(24, oldfd, newfd, flags);
}

static long exit_group(int code) {
    return syscall1(94, code);
}

static long getpid_proc(void) {
    return syscall1(172, 0);
}

static void my_memcpy(void* dst, const void* src, long n) {
    for (long i = 0; i < n; i++) ((unsigned char*)dst)[i] = ((const unsigned char*)src)[i];
}

static int my_strlen(const char* s) {
    int n = 0;
    while (s[n]) n++;
    return n;
}

static char* my_strdup(JNIEnv* env, const char* s) {
    if (!s) return 0;
    int len = my_strlen(s) + 1;
    long page = 4096;
    long size = (len + page - 1) & ~(page - 1);
    long addr = mmap_alloc(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (addr < 0) return 0;
    my_memcpy((void*)addr, s, len);
    return (char*)addr;
}

static void my_strfree(JNIEnv* env, char* s) {
    if (!s) return;
    long page = 4096;
    long len = my_strlen(s) + 1;
    long size = (len + page - 1) & ~(page - 1);
    mmap_free(s, size);
}

static void throw_runtime_exception(JNIEnv* env, const char* msg) {
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass) {
        (*env)->ThrowNew(env, exClass, msg);
    }
    return;
}

static int create_subprocess(JNIEnv* env, const char* cmd, const char* cwd,
                              char** argv, char** envp, int* pid_out)
{
    int ptm = open_ptmx();
    if (ptm < 0) {
        ptm = open_ptmx2();
    }
    if (ptm < 0) {
        throw_runtime_exception(env, "Cannot open pseudo-terminal");
        return -1;
    }

    long pid = fork_proc();
    if (pid < 0) {
        close_fd(ptm);
        throw_runtime_exception(env, "fork() failed");
        return -1;
    }

    if (pid == 0) {
        close_fd(ptm);

        int pts = open_ptmx();
        if (pts < 0) pts = open_ptmx2();
        if (pts < 0) exit_group(1);

        unsigned int ptyno = 0;
        if (ioctl_fd(pts, TIOCGPTN, (long)&ptyno) < 0) exit_group(1);

        int unlock = 0;
        if (ioctl_fd(pts, TIOCSPTLCK, (long)&unlock) < 0) exit_group(1);

        char slave_path[32];
        int n = 0;
        {
            char buf[16];
            unsigned int tmp = ptyno;
            if (tmp == 0) { buf[0] = '0'; n = 1; }
            else {
                while (tmp > 0) { buf[n++] = '0' + (tmp % 10); tmp /= 10; }
                for (int i = 0; i < n/2; i++) { char t = buf[i]; buf[i] = buf[n-1-i]; buf[n-1-i] = t; }
            }
        }
        {
            char* p = slave_path;
            const char* prefix = "/dev/pts/";
            while (*prefix) *p++ = *prefix++;
            {
                char buf[16];
                unsigned int tmp = ptyno;
                int dn = 0;
                if (tmp == 0) { buf[dn++] = '0'; }
                else {
                    while (tmp > 0) { buf[dn++] = '0' + (tmp % 10); tmp /= 10; }
                }
                int freed = 0;
                for (int i = 0; i < dn/2; i++) { char t = buf[i]; buf[i] = buf[dn-1-i]; buf[dn-1-i] = t; }
                for (int i = freed; i < dn; i++) { *p++ = buf[i]; freed++; }
                (void)freed;
            }
            *p = 0;
        }

        int slave_fd = (int)syscall3(56, AT_FDCWD, (long)slave_path, O_RDWR | O_CLOEXEC);
        if (slave_fd < 0) exit_group(1);

        close_fd(pts);

        setsid_proc();

        if (cwd && cwd[0]) {
            syscall3(49, AT_FDCWD, (long)cwd, 0);
        }

        dup3_fd(slave_fd, 0, 0);
        dup3_fd(slave_fd, 1, 0);
        dup3_fd(slave_fd, 2, 0);
        if (slave_fd > 2) close_fd(slave_fd);

        execve_proc(cmd, argv, envp);
        exit_group(1);
    }

    *pid_out = (int)pid;
    return ptm;
}

JNIEXPORT jint JNICALL Java_com_on3_terminal_core_TerminalJNI_createSubprocess(
    JNIEnv* env, jclass clazz,
    jstring cmd, jstring cwd,
    jobjectArray args, jobjectArray envVars,
    jintArray processIdArray,
    jint rows, jint columns,
    jint cellWidth, jint cellHeight)
{
    (void)clazz;
    (void)rows;
    (void)columns;
    (void)cellWidth;
    (void)cellHeight;

    const char* cmd_utf = (*env)->GetStringUTFChars(env, cmd, 0);
    const char* cwd_utf = cwd ? (*env)->GetStringUTFChars(env, cwd, 0) : 0;

    jsize argc = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = 0;
    if (argc > 0) {
        long page = 4096;
        long size = ((argc + 1) * sizeof(char*) + page - 1) & ~(page - 1);
        long addr = mmap_alloc(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr < 0) {
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            throw_runtime_exception(env, "Cannot allocate argv array");
            return -1;
        }
        argv = (char**)addr;
        for (int i = 0; i < argc; i++) {
            jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
            const char* utf = (*env)->GetStringUTFChars(env, arg, 0);
            argv[i] = my_strdup(env, utf);
            (*env)->ReleaseStringUTFChars(env, arg, utf);
        }
        argv[argc] = 0;
    }

    jsize envc = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char** envp = 0;
    if (envc > 0) {
        long page = 4096;
        long size = ((envc + 1) * sizeof(char*) + page - 1) & ~(page - 1);
        long addr = mmap_alloc(0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr < 0) {
            for (int i = 0; i < argc; i++) if (argv[i]) my_strfree(env, argv[i]);
            mmap_free(argv, ((argc + 1) * sizeof(char*) + 4095) & ~4095);
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            throw_runtime_exception(env, "Cannot allocate envp array");
            return -1;
        }
        envp = (char**)addr;
        for (int i = 0; i < envc; i++) {
            jstring var = (jstring)(*env)->GetObjectArrayElement(env, envVars, i);
            const char* utf = (*env)->GetStringUTFChars(env, var, 0);
            envp[i] = my_strdup(env, utf);
            (*env)->ReleaseStringUTFChars(env, var, utf);
        }
        envp[envc] = 0;
    }

    int pid = 0;
    int ptm = create_subprocess(env, cmd_utf, cwd_utf, argv, envp, &pid);

    if (ptm >= 0 && processIdArray) {
        jint pid_j = pid;
        (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &pid_j);
    }

    if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);

    for (int i = 0; i < argc; i++) if (argv && argv[i]) my_strfree(env, argv[i]);
    if (argv) mmap_free(argv, ((argc + 1) * sizeof(char*) + 4095) & ~4095);
    for (int i = 0; i < envc; i++) if (envp && envp[i]) my_strfree(env, envp[i]);
    if (envp) mmap_free(envp, ((envc + 1) * sizeof(char*) + 4095) & ~4095);

    return ptm;
}

JNIEXPORT void JNICALL Java_com_on3_terminal_core_TerminalJNI_setPtyWindowSize(
    JNIEnv* env, jclass clazz,
    jint fd, jint rows, jint cols,
    jint cellWidth, jint cellHeight)
{
    (void)env;
    (void)clazz;
    winsize_t ws;
    ws.ws_row = (unsigned short)rows;
    ws.ws_col = (unsigned short)cols;
    ws.ws_xpixel = (unsigned short)cellWidth;
    ws.ws_ypixel = (unsigned short)cellHeight;
    ioctl_fd(fd, TIOCSWINSZ, (long)&ws);
}

JNIEXPORT jint JNICALL Java_com_on3_terminal_core_TerminalJNI_waitFor(
    JNIEnv* env, jclass clazz, jint pid)
{
    (void)env;
    (void)clazz;
    int status;
    while (1) {
        long result = wait4_pid(pid, &status, 0);
        if (result < 0) {
            if ((int)result == -EINTR) continue;
            return -1;
        }
        break;
    }
    return status;
}

JNIEXPORT void JNICALL Java_com_on3_terminal_core_TerminalJNI_close(
    JNIEnv* env, jclass clazz, jint fd)
{
    (void)env;
    (void)clazz;
    close_fd(fd);
}
