#include <jni.h>
#include <stddef.h>
#include <stdint.h>

#define JNIEXPORT __attribute__((visibility("default")))
#define JNICALL

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
#define SIG_BLOCK 0
#define SIG_UNBLOCK 1
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

static long my_open(const char* path, int flags) {
    return syscall6(56, AT_FDCWD, (long)path, flags, 0, 0, 0);
}
static long my_close(int fd) { return syscall6(57, fd, 0, 0, 0, 0, 0); }
static long my_ioctl(int fd, long request, long arg) {
    return syscall6(29, fd, request, arg, 0, 0, 0);
}
static long my_fork(void) {
    return syscall6(220, SIGCHLD, 0, 0, 0, 0, 0);
}
static long my_execve(const char* path, char* const argv[], char* const envp[]) {
    return syscall6(221, (long)path, (long)argv, (long)envp, 0, 0, 0);
}
static long my_wait4(int pid, int* status, int options) {
    return syscall6(260, pid, (long)status, options, 0, 0, 0);
}
static long my_setsid(void) { return syscall6(112, 0, 0, 0, 0, 0, 0); }
static long my_dup3(int oldfd, int newfd, int flags) {
    return syscall6(24, oldfd, newfd, flags, 0, 0, 0);
}
static long my_exit(int code) { return syscall6(93, code, 0, 0, 0, 0, 0); }
static long my_sigprocmask(int how, const void* set, void* oldset) {
    return syscall6(14, how, (long)set, (long)oldset, 0, 0, 0);
}
static long my_getdents64(int fd, void* dirp, int count) {
    return syscall6(61, fd, (long)dirp, count, 0, 0, 0);
}
static long my_chdir(const char* path) {
    return syscall6(49, AT_FDCWD, (long)path, 0, 0, 0, 0);
}
static long my_setenv(const char* name, const char* value, int overwrite) {
    return syscall6(169, (long)name, (long)value, overwrite, 0, 0, 0);
}

struct dirent64 {
    unsigned long long d_ino;
    long long d_off;
    unsigned short d_reclen;
    unsigned char d_type;
    char d_name[];
};

static void my_memcpy(void* dst, const void* src, long n) {
    for (long i = 0; i < n; i++) ((unsigned char*)dst)[i] = ((const unsigned char*)src)[i];
}
static int my_strlen(const char* s) {
    int n = 0; while (s[n]) n++; return n;
}
static int my_atoi(const char* s) {
    int n = 0; while (*s >= '0' && *s <= '9') { n = n * 10 + (*s - '0'); s++; } return n;
}

static char* my_strdup(const char* s) {
    if (!s) return 0;
    int len = my_strlen(s) + 1;
    long size = (len + 4095) & ~4095;
    long addr = syscall6(222, 0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
    if (addr < 0) return 0;
    my_memcpy((void*)addr, s, len);
    return (char*)addr;
}

static void my_strfree(char* s) {
    if (!s) return;
    long len = my_strlen(s) + 1;
    long size = (len + 4095) & ~4095;
    syscall6(215, (long)s, size, 0, 0, 0, 0);
}

static void my_reverse(char* s, int len) {
    for (int i = 0; i < len / 2; i++) {
        char t = s[i]; s[i] = s[len - 1 - i]; s[len - 1 - i] = t;
    }
}

static int my_itoa(int val, char* buf, int bufsz) {
    if (bufsz < 2) return 0;
    int i = 0;
    if (val == 0) { buf[0] = '0'; buf[1] = 0; return 1; }
    while (val > 0 && i < bufsz - 1) {
        buf[i++] = '0' + (val % 10);
        val /= 10;
    }
    buf[i] = 0;
    my_reverse(buf, i);
    return i;
}

static void throw_runtime_exception(JNIEnv* env, const char* msg) {
    jclass exClass = (*env)->FindClass(env, "java/lang/RuntimeException");
    if (exClass) {
        (*env)->ThrowNew(env, exClass, msg);
    }
}

static int create_subprocess(
    JNIEnv* env,
    const char* cmd,
    const char* cwd,
    char* const argv[],
    char** envp,
    int* pProcessId,
    jint rows, jint columns, jint cell_width, jint cell_height)
{
    (void)rows; (void)columns; (void)cell_width; (void)cell_height;

    int ptm = (int)my_open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) {
        ptm = (int)my_open("/dev/pts/ptmx", O_RDWR | O_CLOEXEC);
    }
    if (ptm < 0) {
        throw_runtime_exception(env, "Cannot open pseudo-terminal");
        return -1;
    }

    int unlock = 0;
    if (my_ioctl(ptm, TIOCSPTLCK, (long)&unlock) < 0) {
        my_close(ptm);
        throw_runtime_exception(env, "unlockpt() failed");
        return -1;
    }

    unsigned int ptyno = 0;
    if (my_ioctl(ptm, TIOCGPTN, (long)&ptyno) < 0) {
        my_close(ptm);
        throw_runtime_exception(env, "TIOCGPTN failed");
        return -1;
    }

    char slave_path[32];
    {
        char* p = slave_path;
        const char* prefix = "/dev/pts/";
        while (*prefix) *p++ = *prefix++;
        char num[16];
        int n = my_itoa((int)ptyno, num, sizeof(num));
        for (int i = 0; i < n; i++) *p++ = num[i];
        *p = 0;
    }

    winsize_t ws;
    ws.ws_row = (unsigned short)rows;
    ws.ws_col = (unsigned short)columns;
    ws.ws_xpixel = (unsigned short)(columns * cell_width);
    ws.ws_ypixel = (unsigned short)(rows * cell_height);
    my_ioctl(ptm, TIOCSWINSZ, (long)&ws);

    long pid = my_fork();
    if (pid < 0) {
        my_close(ptm);
        throw_runtime_exception(env, "Fork failed");
        return -1;
    } else if (pid > 0) {
        *pProcessId = (int)pid;
        return ptm;
    }

    my_close(ptm);

    unsigned char sigset[128] = {0};
    for (int i = 0; i < (int)sizeof(sigset); i++) sigset[i] = 0xff;
    my_sigprocmask(SIG_UNBLOCK, sigset, 0);

    my_setsid();

    int pts = (int)my_open(slave_path, O_RDWR);
    if (pts < 0) my_exit(1);

    my_dup3(pts, 0, 0);
    my_dup3(pts, 1, 0);
    my_dup3(pts, 2, 0);

    {
        int self_fd = (int)my_open("/proc/self/fd", O_RDWR | O_CLOEXEC);
        if (self_fd >= 0) {
            char buf[4096];
            long n;
            while ((n = my_getdents64(self_fd, buf, sizeof(buf))) > 0) {
                for (long pos = 0; pos < n; ) {
                    struct dirent64* d = (struct dirent64*)(buf + pos);
                    int fd = my_atoi(d->d_name);
                    if (fd > 2 && fd != self_fd) my_close(fd);
                    pos += d->d_reclen;
                }
            }
            my_close(self_fd);
        }
    }

    if (cwd && cwd[0]) {
        my_chdir(cwd);
    }

    if (envp) {
        for (int i = 0; envp[i]; i++) {
            char* eq = (char*)envp[i];
            while (*eq && *eq != '=') eq++;
            if (*eq == '=') {
                char name[256];
                int nlen = (int)(eq - envp[i]);
                if (nlen < 256 && nlen > 0) {
                    my_memcpy(name, envp[i], nlen);
                    name[nlen] = 0;
                    my_setenv(name, eq + 1, 1);
                }
            }
        }
    }

    my_execve(cmd, argv, envp);
    my_exit(1);
    return -1;
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

    const char* cmd_utf = (*env)->GetStringUTFChars(env, cmd, 0);
    const char* cwd_utf = cwd ? (*env)->GetStringUTFChars(env, cwd, 0) : 0;

    jsize argc = args ? (*env)->GetArrayLength(env, args) : 0;
    char** argv = 0;
    if (argc > 0) {
        long size = ((argc + 1) * sizeof(char*) + 4095) & ~4095;
        long addr = syscall6(222, 0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr < 0) {
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            throw_runtime_exception(env, "Cannot allocate argv");
            return -1;
        }
        argv = (char**)addr;
        for (int i = 0; i < argc; i++) {
            jstring arg = (jstring)(*env)->GetObjectArrayElement(env, args, i);
            const char* utf = (*env)->GetStringUTFChars(env, arg, 0);
            argv[i] = my_strdup(utf);
            (*env)->ReleaseStringUTFChars(env, arg, utf);
        }
        argv[argc] = 0;
    }

    jsize envc = envVars ? (*env)->GetArrayLength(env, envVars) : 0;
    char** envp = 0;
    if (envc > 0) {
        long size = ((envc + 1) * sizeof(char*) + 4095) & ~4095;
        long addr = syscall6(222, 0, size, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (addr < 0) {
            for (int i = 0; i < argc; i++) if (argv && argv[i]) my_strfree(argv[i]);
            if (argv) syscall6(215, (long)argv, ((argc + 1) * sizeof(char*) + 4095) & ~4095, 0, 0, 0, 0);
            if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
            (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);
            throw_runtime_exception(env, "Cannot allocate envp");
            return -1;
        }
        envp = (char**)addr;
        for (int i = 0; i < envc; i++) {
            jstring var = (jstring)(*env)->GetObjectArrayElement(env, envVars, i);
            const char* utf = (*env)->GetStringUTFChars(env, var, 0);
            envp[i] = my_strdup(utf);
            (*env)->ReleaseStringUTFChars(env, var, utf);
        }
        envp[envc] = 0;
    }

    int pid = 0;
    int ptm = create_subprocess(env, cmd_utf, cwd_utf, argv, envp,
                                 &pid, rows, columns, cellWidth, cellHeight);

    if (cwd_utf) (*env)->ReleaseStringUTFChars(env, cwd, cwd_utf);
    (*env)->ReleaseStringUTFChars(env, cmd, cmd_utf);

    if (argv) {
        for (int i = 0; i < argc; i++) if (argv[i]) my_strfree(argv[i]);
        syscall6(215, (long)argv, ((argc + 1) * sizeof(char*) + 4095) & ~4095, 0, 0, 0, 0);
    }
    if (envp) {
        for (int i = 0; i < envc; i++) if (envp[i]) my_strfree(envp[i]);
        syscall6(215, (long)envp, ((envc + 1) * sizeof(char*) + 4095) & ~4095, 0, 0, 0, 0);
    }

    if (ptm >= 0 && processIdArray) {
        jint pid_j = pid;
        (*env)->SetIntArrayRegion(env, processIdArray, 0, 1, &pid_j);
    }

    return ptm;
}

JNIEXPORT void JNICALL Java_com_on3_terminal_core_TerminalJNI_setPtyWindowSize(
    JNIEnv* env, jclass clazz,
    jint fd, jint rows, jint cols,
    jint cellWidth, jint cellHeight)
{
    (void)env; (void)clazz;
    winsize_t ws;
    ws.ws_row = (unsigned short)rows;
    ws.ws_col = (unsigned short)cols;
    ws.ws_xpixel = (unsigned short)(cols * cellWidth);
    ws.ws_ypixel = (unsigned short)(rows * cellHeight);
    my_ioctl(fd, TIOCSWINSZ, (long)&ws);
}

JNIEXPORT jint JNICALL Java_com_on3_terminal_core_TerminalJNI_waitFor(
    JNIEnv* env, jclass clazz, jint pid)
{
    (void)env; (void)clazz;
    int status;
    while (1) {
        long result = my_wait4(pid, &status, 0);
        if (result < 0) {
            if ((int)result == -EINTR) continue;
            return -1;
        }
        break;
    }
    if (status & 0x7f) {
        return -(status & 0x7f);
    }
    return (status >> 8) & 0xff;
}

JNIEXPORT void JNICALL Java_com_on3_terminal_core_TerminalJNI_close(
    JNIEnv* env, jclass clazz, jint fd)
{
    (void)env; (void)clazz;
    my_close(fd);
}
