package com.github.tvbox.osc.viewmodel.drive;

import androidx.lifecycle.ViewModel;

import com.github.tvbox.osc.bean.DriveFolderFile;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public abstract class AbstractDriveViewModel extends ViewModel {

    protected DriveFolderFile currentDrive = null;
    protected DriveFolderFile currentDriveNote = null;
    protected int sortType = 0;

    public DriveFolderFile getCurrentDrive() {
        return currentDrive;
    }

    public void setCurrentDrive(DriveFolderFile currentDrive) {
        this.currentDrive = currentDrive;
    }

    public DriveFolderFile getCurrentDriveNote() {
        return currentDriveNote;
    }

    public void setCurrentDriveNote(DriveFolderFile currentDriveNote) {
        this.currentDriveNote = currentDriveNote;
    }

    public void setSortType(int sortType) {
        this.sortType = sortType;
    }

    public abstract String loadData(LoadDataCallback callback);

    public abstract Runnable search(String keyword, LoadDataCallback callback);

    protected void sortData(List<DriveFolderFile> data) {
        DriveFolderFile backItem = null;
        if (data.size() > 0 && data.get(0).name == null)
            backItem = data.remove(0);
        Collections.sort(data, sortComparator);
        if (backItem != null)
            data.add(0, backItem);
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

    public interface LoadDataCallback {

        void callback(List<DriveFolderFile> list, boolean alreadyHasChildren);

        void fail(String message);
    }
}
