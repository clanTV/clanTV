package com.github.tvbox.osc.viewmodel.drive;

import com.github.tvbox.osc.bean.DriveFolderFile;
import com.google.gson.JsonObject;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class WebDAVDriveViewModel extends AbstractDriveViewModel {

    private Sardine webDAV;

    private boolean initWebDav() {
        if (webDAV != null)
            return true;
        try {
            JsonObject config = currentDrive.getConfig();
            webDAV = new OkHttpSardine();
            if (config.has("username") && config.has("password")) {
                webDAV.setCredentials(config.get("username").getAsString(), config.get("password").getAsString());
            }
            return true;
        } catch (Exception ex) {
        }
        return false;
    }

    private Sardine getWebDAV() {
        if (initWebDav()) {
            return webDAV;
        }
        return null;
    }

    @Override
    public String loadData(LoadDataCallback callback) {
        JsonObject config = currentDrive.getConfig();
        if (currentDriveNote == null) {
            currentDriveNote = new DriveFolderFile(null,
                    config.has("initPath") ? config.get("initPath").getAsString() : "", false, null, null);
        }
        String targetPath = currentDriveNote.getAccessingPathStr() + currentDriveNote.name;
        if (currentDriveNote.getChildren() == null) {
            new Thread() {
                public void run() {
                    Sardine webDAV = getWebDAV();
                    if (webDAV == null && callback != null) {
                        callback.fail("无法访问该WebDAV地址");
                        return;
                    }
                    List<DavResource> files = null;
                    try {
                        files = webDAV.list(config.get("url").getAsString() + targetPath);
                    } catch (Exception ex) {
                        if (callback != null)
                            callback.fail("无法访问该WebDAV地址");
                        return;
                    }

                    List<DriveFolderFile> items = new ArrayList<>();
                    if (files != null) {
                        for (DavResource file : files) {
                            if (targetPath != null && file.getPath().toUpperCase(Locale.ROOT).endsWith(targetPath.toUpperCase(Locale.ROOT) + "/"))
                                continue;
                            int extNameStartIndex = file.getName().lastIndexOf(".");
                            items.add(new DriveFolderFile(currentDriveNote, file.getName(), !file.isDirectory(),
                                    !file.isDirectory() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                            file.getName().substring(extNameStartIndex + 1) : null,
                                    file.getModified().getTime()));
                        }
                    }
                    sortData(items);
                    DriveFolderFile backItem = new DriveFolderFile(null, null, false, null, null);
                    backItem.parentFolder = backItem;
                    items.add(0, backItem);
                    currentDriveNote.setChildren(items);
                    if (callback != null)
                        callback.callback(currentDriveNote.getChildren(), false);

                }
            }.start();
            return targetPath;
        } else {
            sortData(currentDriveNote.getChildren());
            if (callback != null)
                callback.callback(currentDriveNote.getChildren(), true);
        }
        return targetPath;
    }

    @Override
    public Runnable search(String keyword, LoadDataCallback callback) {
        //not implemented yet
        return new Runnable() {
            @Override
            public void run() {
                if (callback != null)
                    callback.callback(null, false);
            }
        };
    }
}
