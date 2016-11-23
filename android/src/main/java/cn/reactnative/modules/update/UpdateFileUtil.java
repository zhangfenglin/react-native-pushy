package cn.reactnative.modules.update;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.storage.StorageManager;
import android.text.TextUtils;

import java.io.File;
import java.lang.reflect.Method;

public class UpdateFileUtil {
    private final static String DIR_ROOT = File.separator + ".pushy";
    private final static String DIR_APK = DIR_ROOT + File.separator + "Apk";


    public static String getDiskCacheDir(Context context, String uniqueName) {
        final String cachePath = isExternalStorageEnable() ? context.getExternalCacheDir().getPath() : context.getCacheDir().getPath();

        if (TextUtils.isEmpty(cachePath))
            return null;

        return TextUtils.isEmpty(uniqueName) ? cachePath : cachePath + File.separator + uniqueName;
    }

    public static boolean isExternalStorageEnable() {
        return isExternalStorageMounted() || !isExternalStorageRemovable();
    }

    public static boolean isExternalStorageMounted() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * Check if external storage is built-in or removable.
     *
     * @return True if external storage is removable (like an SD card), false
     * otherwise.
     */
    public static boolean isExternalStorageRemovable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            return Environment.isExternalStorageRemovable();
        }
        return true;
    }

    // 新建文件夹
    public static boolean makeDir(String path) {
        if (!isExternalStorageEnable()) {
            return false;
        }
        File file = new File(path);
        if (!file.exists()) {
            file.mkdirs();// 创建文件夹
        }
        return true;
    }

    /**
     * 获取当前可用存储设备路径
     *
     * @param context
     * @return
     */
    public static String getExternalDir(Context context) {
        if (isExternalStorageEnable()) //外置sd卡
            return Environment.getExternalStorageDirectory().getAbsolutePath();

        //其他可用存储卡
        StorageManager storageManager = (StorageManager) context.getSystemService(Context.STORAGE_SERVICE);
        try {
            Class<?>[] paramClasses = {};
            Method getVolumePathsMethod = StorageManager.class.getMethod("getVolumePaths", paramClasses);
            getVolumePathsMethod.setAccessible(true);
            Object[] params = {};
            Object invoke = getVolumePathsMethod.invoke(storageManager, params);
            String esd = Environment.getExternalStorageDirectory().getPath();
            String extSdCard = null;
            for (int i = 0; i < ((String[]) invoke).length; i++) {
                System.out.println(((String[]) invoke)[i]);
                if (((String[]) invoke)[i].equals(esd)) {
                    continue;
                }
                File sdFile = new File(((String[]) invoke)[i]);
                if (sdFile.canWrite()) {
                    extSdCard = ((String[]) invoke)[i]; //获得路径
                    break;
                }
            }
            return extSdCard;
        } catch (Throwable e1) {
            e1.printStackTrace();
        }

        return null;
    }

    public static File getApkFile(Context c, String fileName) {
        File fileDir = new File(getExternalDir(c) + DIR_APK);
        File file = null;
        try {
            if (!fileDir.exists()) {
                if (fileDir.mkdirs())
                    return null;

                if (fileDir.createNewFile())
                    return null;
            }

            file = new File(fileDir, fileName + ".apk");
            if (file.exists())
                file.delete();
            file.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return file;
    }

    /**
     * install APK
     */
    public static void installApk(Context mContext, String filepath) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setDataAndType(Uri.parse("file://" + filepath), "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    public static boolean isApkCanInstall(Context mContext, String filePath) {
        File apkfile = new File(filePath);
        if (!apkfile.exists())
            return false;

        try {
            PackageManager pm = mContext.getPackageManager();
            PackageInfo info = pm.getPackageArchiveInfo(apkfile.getPath(), PackageManager.GET_ACTIVITIES);
            return info != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 删除文件
     * @param path
     * @return
     */
    public static void deleteFileByPath(String path) {
        if (!TextUtils.isEmpty(path)) {
            File file = new File(path);
            deleteFile(file);
        }
    }

    /**
     * 删除文件
     * @param file
     * @return
     */
    public static void deleteFile(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }

    public static void clearCache(Context c) {
        File fileDir = new File(getExternalDir(c) + DIR_APK);
        if (fileDir.exists()) {
            File[] files = fileDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.exists()) {
                        file.delete();
                    }
                }
            }
        }
    }
}
