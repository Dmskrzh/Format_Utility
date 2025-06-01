package gsu;

public class ClusterUtils {

    // Метод получения допустимых размеров кластера
    public static int[] getAllowedClusterSizes(String fileSystem, long driveSize) {
        switch (fileSystem) {
            case "NTFS":
                return new int[]{512, 1024, 2048, 4096, 8192, 16384, 32768, 65536};
            case "FAT32":
                return getFAT32AllowedClusterSizes(driveSize);
            case "exFAT":
                return new int[]{512, 1024, 2048, 4096, 8192, 16384, 32768, 65536, 131072, 262144};
            default:
                return new int[]{4096};
        }
    }

    // Получение допустимых размеров кластера для FAT32 в зависимости от ёмкости накопителя
    private static int[] getFAT32AllowedClusterSizes(long driveSize) {
        double sizeGB = driveSize / (1024.0 * 1024.0 * 1024.0);

        if (sizeGB <= 0.5) {
            return new int[]{512};
        } else if (sizeGB <= 1) {
            return new int[]{512, 1024, 2048, 4096, 8192};
        } else if (sizeGB <= 2) {
            return new int[]{512, 1024, 2048, 4096, 8192, 16384};
        } else if (sizeGB <= 4) {
            return new int[]{1024, 2048, 4096, 8192, 16384, 32768};
        } else if (sizeGB <= 8) {
            return new int[]{2048, 4096, 8192, 16384, 32768, 65536};
        } else if (sizeGB <= 16) {
            return new int[]{4096, 8192, 16384, 32768, 65536};
        } else if (sizeGB <= 32) {
            return new int[]{8192, 16384, 32768, 65536};
        } else {
            return new int[]{8192, 16384, 32768};
        }
    }
}