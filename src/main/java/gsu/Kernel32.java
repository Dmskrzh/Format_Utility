package gsu;

import com.sun.jna.Native;
import com.sun.jna.WString;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.LongByReference;
import com.sun.jna.win32.StdCallLibrary;

// Интерфейс для доступа к функциям Windows API с помощью JNA
public interface Kernel32 extends StdCallLibrary {
    Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

    // Константы для работы с дисками
    int DRIVE_REMOVABLE = 2;
    int DRIVE_FIXED = 3;

    // Получение типа диска
    int GetDriveTypeW(WString lpRootPathName);

    // Получение списка доступных дисков
    int GetLogicalDrives();

    // Получение информации о томе
    boolean GetVolumeInformationW(
            WString lpRootPathName,
            char[] lpVolumeNameBuffer,
            int nVolumeNameSize,
            IntByReference lpVolumeSerialNumber,
            IntByReference lpMaximumComponentLength,
            IntByReference lpFileSystemFlags,
            char[] lpFileSystemNameBuffer,
            int nFileSystemNameSize
    );

    // Получение объёма свободного места на диске
    boolean GetDiskFreeSpaceExW(
            WString lpDirectoryName,
            LongByReference lpFreeBytesAvailableToCaller,
            LongByReference lpTotalNumberOfBytes,
            LongByReference lpTotalNumberOfFreeBytes
    );
}