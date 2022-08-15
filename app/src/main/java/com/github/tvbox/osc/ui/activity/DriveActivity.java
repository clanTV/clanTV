package com.github.tvbox.osc.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.ColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DiffUtil;
import androidx.viewpager.widget.ViewPager;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.github.tvbox.osc.R;
import com.github.tvbox.osc.api.ApiConfig;
import com.github.tvbox.osc.base.App;
import com.github.tvbox.osc.base.BaseActivity;
import com.github.tvbox.osc.base.BaseLazyFragment;
import com.github.tvbox.osc.bean.AbsSortXml;
import com.github.tvbox.osc.bean.DriveFolderFile;
import com.github.tvbox.osc.bean.Movie;
import com.github.tvbox.osc.bean.MovieSort;
import com.github.tvbox.osc.bean.VodInfo;
import com.github.tvbox.osc.cache.RoomDataManger;
import com.github.tvbox.osc.cache.StorageDrive;
import com.github.tvbox.osc.event.RefreshEvent;
import com.github.tvbox.osc.ui.adapter.DriveAdapter;
import com.github.tvbox.osc.ui.adapter.HomePageAdapter;
import com.github.tvbox.osc.ui.adapter.SelectDialogAdapter;
import com.github.tvbox.osc.ui.adapter.SortAdapter;
import com.github.tvbox.osc.ui.dialog.SelectDialog;
import com.github.tvbox.osc.ui.dialog.WebdavDialog;
import com.github.tvbox.osc.ui.fragment.GridFragment;
import com.github.tvbox.osc.ui.tv.widget.DefaultTransformer;
import com.github.tvbox.osc.ui.tv.widget.FixedSpeedScroller;
import com.github.tvbox.osc.ui.tv.widget.NoScrollViewPager;
import com.github.tvbox.osc.util.FastClickCheckUtil;
import com.github.tvbox.osc.util.HawkConfig;
import com.github.tvbox.osc.util.PlayerHelper;
import com.github.tvbox.osc.util.StorageDriveType;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.obsez.android.lib.filechooser.ChooserDialog;
import com.orhanobut.hawk.Hawk;
import com.owen.tvrecyclerview.widget.TvRecyclerView;
import com.owen.tvrecyclerview.widget.V7LinearLayoutManager;
import com.thegrizzlylabs.sardineandroid.DavResource;
import com.thegrizzlylabs.sardineandroid.Sardine;

import org.chromium.ui.widget.ButtonCompat;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.lang.reflect.Field;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import jcifs.smb.SmbFile;
import me.jessyan.autosize.utils.AutoSizeUtils;

public class DriveActivity extends BaseActivity {

    private TextView txtTitle;
    private TvRecyclerView mGridView;
    private ImageButton btnAddServer;
    private ImageButton btnRemoveServer;
    private ImageButton btnSort;
    private DriveAdapter adapter = new DriveAdapter();
    private List<DriveFolderFile> drives = null;
    private DriveFolderFile currentDrive = null;
    private DriveFolderFile driveNode = null;
    private int sortType = 0;

    private boolean isRight;
    private boolean sortChange = false;

    private int currentSelected = 0;
    private int sortFocused = 0;
    private boolean delMode = false;
    public View sortFocusView = null;

    private Handler mHandler = new Handler();

    @Override
    protected int getLayoutResID() {
        return R.layout.activity_drive;
    }

    @Override
    protected void init() {
        initView();
        initData();
    }

    private void initView() {
        EventBus.getDefault().register(this);
        this.txtTitle = findViewById(R.id.textView);
        this.btnAddServer = findViewById(R.id.btnAddServer);
        this.mGridView = findViewById(R.id.mGridView);
        this.btnRemoveServer = findViewById(R.id.btnRemoveServer);
        this.btnSort = findViewById(R.id.btnSort);
        this.btnRemoveServer.setColorFilter(ContextCompat.getColor(mContext, R.color.color_FFFFFF));
        this.btnRemoveServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleDelMode();
            }
        });
        findViewById(R.id.btnHome).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DriveActivity.super.onBackPressed();
            }
        });
        this.btnSort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                openSortDialog();
            }
        });
        this.btnAddServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FastClickCheckUtil.check(view);
                StorageDriveType.TYPE[] types = StorageDriveType.TYPE.values();
                SelectDialog<StorageDriveType.TYPE> dialog = new SelectDialog<>(DriveActivity.this);
                dialog.setTip("请选择存盘类型");
                dialog.setItemCheckDisplay(false);
                String[] typeNames = StorageDriveType.getTypeNames();
                dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<StorageDriveType.TYPE>() {
                    @Override
                    public void click(StorageDriveType.TYPE value, int pos) {
                        if (value == StorageDriveType.TYPE.LOCAL) {
                            if (Build.VERSION.SDK_INT >= 23) {
                                if (App.getInstance().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    ActivityCompat.requestPermissions(DriveActivity.this,
                                            new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                                    return;
                                }
                            }
                            openFilePicker();
                            dialog.dismiss();
                        }
                        if (value == StorageDriveType.TYPE.WEBDAV) {
                            openWebdavDialog(null);
                            dialog.dismiss();
                        }
                    }

                    @Override
                    public String getDisplay(StorageDriveType.TYPE val) {
                        return typeNames[val.ordinal()];
                    }
                }, new DiffUtil.ItemCallback<StorageDriveType.TYPE>() {
                    @Override
                    public boolean areItemsTheSame(@NonNull @NotNull StorageDriveType.TYPE oldItem, @NonNull @NotNull StorageDriveType.TYPE newItem) {
                        return oldItem.equals(newItem);
                    }

                    @Override
                    public boolean areContentsTheSame(@NonNull @NotNull StorageDriveType.TYPE oldItem, @NonNull @NotNull StorageDriveType.TYPE newItem) {
                        return oldItem.equals(newItem);
                    }
                }, Arrays.asList(types), 0);
                dialog.show();
            }
        });
        this.mGridView.setLayoutManager(new V7LinearLayoutManager(this.mContext, V7LinearLayoutManager.VERTICAL, false));
        this.mGridView.setSpacingWithMargins(AutoSizeUtils.mm2px(this.mContext, 5), 0);
        this.mGridView.setAdapter(this.adapter);
        this.adapter.bindToRecyclerView(this.mGridView);
        this.mGridView.setOnItemListener(new TvRecyclerView.OnItemListener() {
            public void onItemPreSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (position >= 0)
                    adapter.getData().get(position).isSelected = false;
            }

            public void onItemSelected(TvRecyclerView tvRecyclerView, View view, int position) {
                if (position >= 0)
                    adapter.getData().get(position).isSelected = true;
            }

            @Override
            public void onItemClick(TvRecyclerView parent, View itemView, int position) {
                if (delMode) {
                    DriveFolderFile selectedDrive = drives.get(position);
                    RoomDataManger.deleteDrive(selectedDrive.getDriveData().getId());
                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_DRIVE_REFRESH));
                    return;
                }
                btnAddServer.setVisibility(View.GONE);
                btnRemoveServer.setVisibility(View.GONE);
                if (currentDrive == null) {
                    currentDrive = drives.get(position);
                    initSwitch();
                } else {
                    DriveFolderFile selectedItem = DriveActivity.this.adapter.getItem(position);
                    if (selectedItem == selectedItem.parentFolder && selectedItem.name == null) {
                        returnPreviousFolder();
                    } else if (!selectedItem.isFile) {
                        driveNode = selectedItem;
                        initSwitch();
                    } else {
                        // takagen99 - To only play media file
                        if (StorageDriveType.isVideoType(selectedItem.fileType)) {
                            if (currentDrive.getDriveType() == StorageDriveType.TYPE.LOCAL) {
                                 playFile(currentDrive.name + selectedItem.getAccessingPathStr() + selectedItem.name);
                            } else if (currentDrive.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
                                JsonObject config = currentDrive.getConfig();
                                String targetPath = selectedItem.getAccessingPathStr() + selectedItem.name;
                                playFile(config.get("url").getAsString() + targetPath);
                            }
                        } else {
                            Toast.makeText(DriveActivity.this, "Media Unsupported", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        });
        setLoadSir(findViewById(R.id.mLayout));
    }

    private void playFile(String fileUrl) {
        VodInfo vodInfo = new VodInfo();
        vodInfo.name = "存储";
        vodInfo.playFlag = "drive";
        if (currentDrive.getDriveType() == StorageDriveType.TYPE.WEBDAV) {
            String credentialStr = currentDrive.getWebDAVBase64Credential();
            if (credentialStr != null) {
                JsonObject playerConfig = new JsonObject();
                JsonArray headers = new JsonArray();
                JsonElement authorization = JsonParser.parseString(
                        "{ \"name\": \"authorization\", \"value\": \"Basic " + credentialStr + "\" }");
                headers.add(authorization);
                playerConfig.add("headers", headers);
                vodInfo.playerCfg = playerConfig.toString();
            }
        }
        vodInfo.seriesFlags = new ArrayList<>();
        vodInfo.seriesFlags.add(new VodInfo.VodSeriesFlag("drive"));
        vodInfo.seriesMap = new LinkedHashMap<>();
        VodInfo.VodSeries series = new VodInfo.VodSeries(fileUrl, "tvbox-drive://" + fileUrl);
        List<VodInfo.VodSeries> seriesList = new ArrayList<>();
        seriesList.add(series);
        vodInfo.seriesMap.put("drive", seriesList);
        Bundle bundle = new Bundle();
        bundle.putBoolean("newSource", true);
        bundle.putString("sourceKey", "_drive");
        bundle.putSerializable("VodInfo", vodInfo);
        // takagen99 - to play file here zzzzzzzzzzzzzzz
        jumpActivity(PlayActivity.class, bundle);
    }

    private void openSortDialog() {
        List<String> options = Arrays.asList(new String[]{"按名字升序", "按名字降序", "按修改时间升序", "按修改时间降序"});
        int sort = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);
        SelectDialog<String> dialog = new SelectDialog<>(DriveActivity.this);
        dialog.setTip("请选择列表排序方式");
        dialog.setAdapter(new SelectDialogAdapter.SelectDialogInterface<String>() {
            @Override
            public void click(String value, int pos) {
                sortType = pos;
                Hawk.put(HawkConfig.STORAGE_DRIVE_SORT, pos);
                dialog.dismiss();
                initSwitch();
            }

            @Override
            public String getDisplay(String val) {
                return val;
            }
        }, null, options, sort);
        dialog.show();
    }

    private Comparator<DriveFolderFile> sortComparator = new Comparator<DriveFolderFile>() {
        @Override
        public int compare(DriveFolderFile o1, DriveFolderFile o2) {
            switch (sortType) {
                case 1:
                    return Collator.getInstance(Locale.CHINESE).compare(o2.name.toUpperCase(Locale.CHINESE), o1.name.toUpperCase(Locale.CHINESE));
                case 2:
                    return Long.compare(o1.lastModifiedDate, o2.lastModifiedDate);
                case 3:
                    return Long.compare(o2.lastModifiedDate, o1.lastModifiedDate);
                default:
                    return Collator.getInstance(Locale.CHINESE).compare(o1.name.toUpperCase(Locale.CHINESE), o2.name.toUpperCase(Locale.CHINESE));
            }
        }
    };

    private void sortData(List<DriveFolderFile> data) {
        DriveFolderFile backItem = null;
        if (data.size() > 0 && data.get(0).name == null)
            backItem = data.remove(0);
        Collections.sort(data, sortComparator);
        if (backItem != null)
            data.add(0, backItem);
    }

    private void openFilePicker() {
        if (delMode)
            toggleDelMode();
        ChooserDialog dialog = new ChooserDialog(mContext, R.style.FileChooserStyle);
        dialog
                .withStringResources("选择一个文件夹", "确定", "取消")
                .titleFollowsDir(true)
                .displayPath(true)
                .enableDpad(true)
                .withFilter(true, true)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String dir, File dirFile) {
                        String absPath = dirFile.getAbsolutePath();
                        for (DriveFolderFile drive : drives) {
                            if (drive.getDriveType() == StorageDriveType.TYPE.LOCAL && absPath.equals(drive.getDriveData().name)) {
                                Toast.makeText(DriveActivity.this, "此文件夹之前已被添加到空间列表！", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                        RoomDataManger.insertDriveRecord(absPath, StorageDriveType.TYPE.LOCAL, null);
                        EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_DRIVE_REFRESH));
                        return;
                    }
                }).show();
//        FilePickerManager
//                .from(this)
//                .filter(new AbstractFileFilter() {
//                    @NonNull
//                    @Override
//                    public ArrayList<FileItemBeanImpl> doFilter(@NonNull ArrayList<FileItemBeanImpl> arrayList) {
//                        ArrayList<FileItemBeanImpl> filteredList = new ArrayList<>();
//                        for (FileItemBeanImpl bean : arrayList) {
//                            if(bean.isDir())
//                                filteredList.add(bean);
//                        }
//                        return filteredList;
//                    }
//                })
//                .skipDirWhenSelect(false)
//                .forResult(FilePickerManager.REQUEST_CODE);
    }

    private void openWebdavDialog(StorageDrive drive) {
        WebdavDialog webdavDialog = new WebdavDialog(mContext, drive);
        webdavDialog.show();
    }

    public void toggleDelMode() {
        delMode = !delMode;
        if (delMode) {
            this.btnRemoveServer.setColorFilter(ContextCompat.getColor(mContext, R.color.color_FF0057));
        } else {
            this.btnRemoveServer.setColorFilter(ContextCompat.getColor(mContext, R.color.color_FFFFFF));
        }
        adapter.toggleDelMode(delMode);
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == FilePickerManager.REQUEST_CODE) {
//            if (resultCode == Activity.RESULT_OK) {
//                List<String> paths = FilePickerManager.obtainData();
//                if(paths.size() > 0) {
//                    List<String> existingPaths = new ArrayList<>();
//                    for(DriveFolderFile drive : drives) {
//                        if(drive.getDriveType() == StorageDriveType.TYPE.LOCAL)
//                            existingPaths.add(drive.getDriveData().name);
//                    }
//                    for (String path : paths) {
//                        boolean foundExist = false;
//                        for(String existingPath : existingPaths) {
//                            if(path.equals(existingPath)) {
//                                foundExist = true;
//                                break;
//                            }
//                        }
//                        if(foundExist)
//                            continue;
//                        RoomDataManger.insertDriveRecord(path, StorageDriveType.TYPE.LOCAL, null);
//                    }
//                    EventBus.getDefault().post(new RefreshEvent(RefreshEvent.TYPE_DRIVE_REFRESH));
//                    return;
//                }
//            }
//            Toast.makeText(this, "没有选择任何本地目录哦！", Toast.LENGTH_SHORT).show();
//        }
//    }

    private void initData() {
        this.txtTitle.setText(getString(R.string.act_drive));
        sortType = Hawk.get(HawkConfig.STORAGE_DRIVE_SORT, 0);
        btnSort.setVisibility(View.GONE);
        if (drives == null) {
            drives = new ArrayList<>();
            List<StorageDrive> storageDrives = RoomDataManger.getAllDrives();
            for (StorageDrive storageDrive : storageDrives) {
                DriveFolderFile drive = new DriveFolderFile(storageDrive);
                if (delMode)
                    drive.isDelMode = true;
                drives.add(drive);
            }
        }
        adapter.setNewData(drives);
        this.setSelectedItem(drives);
        this.btnAddServer.setVisibility(View.VISIBLE);
        this.btnRemoveServer.setVisibility(View.VISIBLE);
        showSuccess();
    }

    private void setSelectedItem(List<DriveFolderFile> list) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).isSelected) {
                int isIndex = i;
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mGridView.setSelection(isIndex);
                    }
                }, 50);
                return;
            }
        }
        mGridView.setSelection(0);
    }

    private void initLocal() {
        if (driveNode == null)
            driveNode = new DriveFolderFile(null, "", false, null, null);
        String path = currentDrive.name + driveNode.getAccessingPathStr() + driveNode.name;
        this.txtTitle.setText(path);
        if (driveNode.getChildren() == null) {
            File[] files = (new File(path)).listFiles();
            List<DriveFolderFile> items = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    int extNameStartIndex = file.getName().lastIndexOf(".");
                    items.add(new DriveFolderFile(driveNode, file.getName(), file.isFile(),
                            file.isFile() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                    file.getName().substring(extNameStartIndex + 1) : null,
                            file.lastModified()));
                }
            }
            sortData(items);
            DriveFolderFile backItem = new DriveFolderFile(null, null, false, null, null);
            backItem.parentFolder = backItem;
            items.add(0, backItem);
            driveNode.setChildren(items);
            adapter.setNewData(driveNode.getChildren());
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mGridView.setSelection(0);
                }
            }, 50);
        } else {
            sortData(driveNode.getChildren());
            adapter.setNewData(driveNode.getChildren());
            setSelectedItem(driveNode.getChildren());
        }
    }

    private void initWebDAV() {
        JsonObject config = currentDrive.getConfig();
        if (driveNode == null) {
            driveNode = new DriveFolderFile(null,
                    config.has("initPath") ? config.get("initPath").getAsString() : "", false, null, null);
        }
        if (driveNode.getChildren() == null) {
            showLoading();
            new Thread() {
                public void run() {
                    Sardine webDAV = currentDrive.getWebDAV();
                    if (webDAV == null) {
                        showSuccess();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "无法访问该WebDAV地址", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    List<DavResource> files = null;
                    String targetPath = driveNode.getAccessingPathStr() + driveNode.name;
                    try {
                        files = webDAV.list(config.get("url").getAsString() + targetPath);
                    } catch (Exception ex) {
                        showSuccess();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "无法访问该WebDAV地址", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    List<DriveFolderFile> items = new ArrayList<>();
                    if (files != null) {
                        for (DavResource file : files) {
                            if (targetPath != null && file.getPath().toUpperCase(Locale.ROOT).endsWith(targetPath.toUpperCase(Locale.ROOT) + "/"))
                                continue;
                            int extNameStartIndex = file.getName().lastIndexOf(".");
                            items.add(new DriveFolderFile(driveNode, file.getName(), !file.isDirectory(),
                                    !file.isDirectory() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                            file.getName().substring(extNameStartIndex + 1) : null,
                                    file.getModified().getTime()));
                        }
                    }
                    sortData(items);
                    DriveFolderFile backItem = new DriveFolderFile(null, null, false, null, null);
                    backItem.parentFolder = backItem;
                    items.add(0, backItem);
                    driveNode.setChildren(items);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            adapter.setNewData(driveNode.getChildren());
                            DriveActivity.this.txtTitle.setText(currentDrive.name + ": " + targetPath);
                            mGridView.setSelection(0);
                            showSuccess();
                        }
                    }, 50);

                }
            }.start();
        } else {
            String targetPath = driveNode.getAccessingPathStr() + driveNode.name;
            DriveActivity.this.txtTitle.setText(currentDrive.name + ": " + targetPath);
            sortData(driveNode.getChildren());
            adapter.setNewData(driveNode.getChildren());
            setSelectedItem(driveNode.getChildren());
        }
    }

    private void initSMB() {
        JsonObject config = currentDrive.getConfig();
        if (driveNode == null) {
            driveNode = new DriveFolderFile(null, "", false, null, null);
        }
        if (driveNode.getChildren() == null) {
            showLoading();
            new Thread() {
                public void run() {
                    Sardine webDAV = currentDrive.getWebDAV();
                    if (webDAV == null) {
                        showSuccess();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "无法访问该WebDAV地址", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }
                    List<DavResource> files = null;
                    String targetPath = driveNode.getAccessingPathStr() + driveNode.name;
                    try {
                        files = webDAV.list(config.get("url").getAsString() + targetPath);
                    } catch (Exception ex) {
                        showSuccess();
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(mContext, "无法访问该WebDAV地址", Toast.LENGTH_SHORT).show();
                            }
                        });
                        return;
                    }

                    List<DriveFolderFile> items = new ArrayList<>();
                    if (files != null) {
                        for (DavResource file : files) {
                            if (targetPath != null && file.getPath().toUpperCase(Locale.ROOT).endsWith(targetPath.toUpperCase(Locale.ROOT) + "/"))
                                continue;
                            int extNameStartIndex = file.getName().lastIndexOf(".");
                            items.add(new DriveFolderFile(driveNode, file.getName(), !file.isDirectory(),
                                    !file.isDirectory() && extNameStartIndex >= 0 && extNameStartIndex < file.getName().length() ?
                                            file.getName().substring(extNameStartIndex + 1) : null,
                                    file.getModified().getTime()));
                        }
                    }
                    sortData(items);
                    DriveFolderFile backItem = new DriveFolderFile(null, null, false, null, null);
                    backItem.parentFolder = backItem;
                    items.add(0, backItem);
                    driveNode.setChildren(items);
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            adapter.setNewData(driveNode.getChildren());
                            DriveActivity.this.txtTitle.setText(currentDrive.name + ": " + targetPath);
                            mGridView.setSelection(0);
                            showSuccess();
                        }
                    }, 50);

                }
            }.start();
        } else {
            String targetPath = driveNode.getAccessingPathStr() + driveNode.name;
            DriveActivity.this.txtTitle.setText(currentDrive.name + ": " + targetPath);
            sortData(driveNode.getChildren());
            adapter.setNewData(driveNode.getChildren());
            setSelectedItem(driveNode.getChildren());
        }
    }

    private void returnPreviousFolder() {
        driveNode.setChildren(null);
        driveNode = driveNode.parentFolder;
        if (driveNode == null) {
            currentDrive = null;
            initData();
            return;
        }
        initSwitch();
    }

    private void initSwitch() {
        btnSort.setVisibility(View.VISIBLE);
        switch (currentDrive.getDriveType()) {
            case LOCAL:
                initLocal();
                break;
            case WEBDAV:
                initWebDAV();
                break;
        }
    }

    @Override
    public void onBackPressed() {
        if (currentDrive != null)
            mGridView.onClick(mGridView.getChildAt(0));
        else if (!delMode)
            super.onBackPressed();
        else
            toggleDelMode();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void refresh(RefreshEvent event) {
        if (event.type == RefreshEvent.TYPE_DRIVE_REFRESH) {
            drives = null;
            initData();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }
}