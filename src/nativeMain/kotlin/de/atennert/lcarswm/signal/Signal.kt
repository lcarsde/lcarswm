package de.atennert.lcarswm.signal

import platform.posix.*

enum class Signal(val signalValue: Int) {
    ABRT(SIGABRT),
    SEGV(SIGSEGV),
    FPE(SIGFPE),
    ILL(SIGILL),
    QUIT(SIGQUIT),
    TRAP(SIGTRAP),
    SYS(SIGSYS),
    BUS(SIGBUS),
    XCPU(SIGXCPU),
    XFSZ(SIGXFSZ),
    USR1(SIGUSR1),
    USR2(SIGUSR2),
    TERM(SIGTERM),
    INT(SIGINT),
    HUP(SIGHUP),
    PIPE(SIGPIPE),
    CHLD(SIGCHLD),
    TTIN(SIGTTIN),
    TTOU(SIGTTOU);
}