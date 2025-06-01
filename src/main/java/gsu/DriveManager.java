package gsu;

import com.sun.jna.WString;
import com.sun.jna.ptr.LongByReference;

import java.util.ArrayList;
import java.util.List;

public class DriveManager {
    // Метод загрузки доступных накопителей
    public static List<DriveInfo> getAvailableDrives() {
        List<DriveInfo> drivesList = new ArrayList<>();
        int drives = Kernel32.INSTANCE.GetLogicalDrives();

        for (char drive = 'A'; drive <= 'Z'; drive++) {
            if ((drives & (1 << (drive - 'A'))) != 0) {
                WString drivePath = new WString(drive + ":\\");

                int driveType = Kernel32.INSTANCE.GetDriveTypeW(drivePath);

                if (driveType == Kernel32.DRIVE_REMOVABLE || driveType == Kernel32.DRIVE_FIXED) {
                    char[] fileSystemName = new char[256];
                    boolean result = Kernel32.INSTANCE.GetVolumeInformationW(
                            drivePath, null, 0, null, null, null,
                            fileSystemName, fileSystemName.length
                    );

                    String fileSystem = result ? new String(fileSystemName).trim() : "Неизвестно";
                    String typeDesc = (driveType == Kernel32.DRIVE_REMOVABLE) ? "Съемный диск" : "Жесткий диск";

                    LongByReference totalBytes = new LongByReference();
                    long driveSize = 0;
                    boolean sizeResult = Kernel32.INSTANCE.GetDiskFreeSpaceExW(
                            drivePath, null, totalBytes, null
                    );

                    if (sizeResult) {
                        driveSize = totalBytes.getValue();
                        double sizeGB = driveSize / (1024.0 * 1024.0 * 1024.0);
                        typeDesc += String.format(" [%s] (%.1f ГБ)", fileSystem, sizeGB);
                    } else {
                        typeDesc += " [" + fileSystem + "]";
                    }

                    drivesList.add(new DriveInfo(String.valueOf(drive), typeDesc, driveSize, driveType));
                }
            }
        }
        return drivesList;
    }
}