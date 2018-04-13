/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.launcher3;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import java.util.Comparator;

public class ShortcutAndWidgetContainer extends ViewGroup {
    static final String TAG = "CellLayoutChildren";

    // These are temporary variables to prevent having to allocate a new object just to
    // return an (x, y) value from helper functions. Do NOT use them to maintain other state.
    private final int[] mTmpCellXY = new int[2];

    private final WallpaperManager mWallpaperManager;

    private boolean mIsHotseatLayout;

    private int mCellWidth;
    private int mCellHeight;

    private int mWidthGap;
    private int mHeightGap;

    private int mCountX;

    private Launcher mLauncher;

    private boolean mInvertIfRtl = false;

    public ShortcutAndWidgetContainer(Context context) {
        super(context);
        mLauncher = Launcher.getLauncher(context);
        mWallpaperManager = WallpaperManager.getInstance(context);
    }

    public void setCellDimensions(int cellWidth, int cellHeight, int widthGap, int heightGap,
                                  int countX, int countY) {
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mWidthGap = widthGap;
        mHeightGap = heightGap;
        mCountX = countX;
    }

    public View getChildAt(int x, int y) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();

            if ((lp.cellX <= x) && (x < lp.cellX + lp.cellHSpan) &&
                    (lp.cellY <= y) && (y < lp.cellY + lp.cellVSpan)) {
                return child;
            }
        }
        return null;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();
        int widthSpecSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSpecSize = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(widthSpecSize, heightSpecSize);

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                measureChild(child);
            }
        }
    }


    public void setupLp(CellLayout.LayoutParams lp, boolean isFromDesktop) {
        if (mIsHotseatLayout) {
            lp.setupForHotSeat(mCellWidth, mCellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                    getChildCount(), isFromDesktop);
            return;
        }
        lp.setup(mCellWidth, mCellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                mCountX);
    }

    // Set whether or not to invert the layout horizontally if the layout is in RTL mode.
    public void setInvertIfRtl(boolean invert) {
        mInvertIfRtl = invert;
    }

    public void setIsHotseat(boolean isHotseat) {
        mIsHotseatLayout = isHotseat;
    }

    int getCellContentWidth() {
        final DeviceProfile grid = mLauncher.getDeviceProfile();
        return Math.min(getMeasuredWidth(), mIsHotseatLayout ?
                grid.hotseatCellWidthPx : grid.cellWidthPx);
    }

    int getCellContentHeight() {
        final DeviceProfile grid = mLauncher.getDeviceProfile();
        return Math.min(getMeasuredHeight(), mIsHotseatLayout ?
                grid.hotseatCellHeightPx : grid.cellHeightPx);
    }


    private boolean isFromDesktop = false;

    public void setIsFromDesktop(boolean isFromDesktop) {
        this.isFromDesktop = isFromDesktop;
    }


    public void measureChild(View child) {
        final DeviceProfile grid = mLauncher.getDeviceProfile();
        final int cellWidth = mCellWidth;
        final int cellHeight = mCellHeight;
        CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
        if (!lp.isFullscreen) {
            if (mIsHotseatLayout) {
                lp.setupForHotSeat(mCellWidth, mCellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                        getChildCount(), isFromDesktop);
            } else {
                lp.setup(cellWidth, cellHeight, mWidthGap, mHeightGap, invertLayoutHorizontally(),
                        mCountX);
            }

            if (child instanceof LauncherAppWidgetHostView) {
                // Widgets have their own padding, so skip
            } else {
                // Otherwise, center the icon/folder
                int cHeight = getCellContentHeight();
                int cellPaddingY = (int) Math.max(0, ((lp.height - cHeight) / 2f));
                int cellPaddingX = (int) (grid.edgeMarginPx / 2f);
                child.setPadding(cellPaddingX, cellPaddingY, cellPaddingX, 0);
            }
        } else {
            lp.x = 0;
            lp.y = 0;
            lp.width = getMeasuredWidth();
            lp.height = getMeasuredHeight();
        }
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(lp.width, MeasureSpec.EXACTLY);
        int childheightMeasureSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        child.measure(childWidthMeasureSpec, childheightMeasureSpec);
    }

    public boolean invertLayoutHorizontally() {
        return mInvertIfRtl && Utilities.isRtl(getResources());
    }

    private Comparator<View> comparator = new Comparator<View>() {
        @Override
        public int compare(View o1, View o2) {
            return ((CellLayout.LayoutParams) o1.getLayoutParams()).x - ((CellLayout.LayoutParams) o2.getLayoutParams()).x;
        }
    };

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
//        if (mIsHotseatLayout) {
//            int count = getChildCount();
//            if (count == 0) return;
//            ArrayList<View> views = new ArrayList<>();
//            int spanX = (r - l) / mCountX;
//            int left = (r - l) / 2 - (count * spanX) / 2;
//            for (int i = 0; i < count; i++) {
//                views.add(getChildAt(i));
//            }
//            Collections.sort(views, comparator);
//            for (int i = 0; i < views.size(); i++) {
//
//                final View child = views.get(i);
//                if (child.getVisibility() != GONE) {
//                    CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
//                    int childLeft = lp.x;
//                    int childTop = lp.y;
//                    child.layout(left, childTop, left + spanX, childTop + lp.height);
//                    left += spanX;
//                    if (lp.dropped) {
//                        lp.dropped = false;
//
//                        final int[] cellXY = mTmpCellXY;
//                        getLocationOnScreen(cellXY);
//                        mWallpaperManager.sendWallpaperCommand(getWindowToken(),
//                                WallpaperManager.COMMAND_DROP,
//                                cellXY[0] + childLeft + lp.width / 2,
//                                cellXY[1] + childTop + lp.height / 2, 0, null);
//                    }
//                }
//            }
//        } else {

        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                CellLayout.LayoutParams lp = (CellLayout.LayoutParams) child.getLayoutParams();
                int childLeft = lp.x;
                int childTop = lp.y;
//                if (mIsHotseatLayout) {
//                    Log.e("ZCY", childLeft + "");
//                }
                child.layout(childLeft, childTop, childLeft + lp.width, childTop + lp.height);

                if (lp.dropped) {
                    lp.dropped = false;

                    final int[] cellXY = mTmpCellXY;
                    getLocationOnScreen(cellXY);
                    mWallpaperManager.sendWallpaperCommand(getWindowToken(),
                            WallpaperManager.COMMAND_DROP,
                            cellXY[0] + childLeft + lp.width / 2,
                            cellXY[1] + childTop + lp.height / 2, 0, null);
                }
            }
        }
//        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return false;
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        super.requestChildFocus(child, focused);
        if (child != null) {
            Rect r = new Rect();
            child.getDrawingRect(r);
            requestRectangleOnScreen(r);
        }
    }

    @Override
    public void cancelLongPress() {
        super.cancelLongPress();

        // Cancel long press for all children
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View child = getChildAt(i);
            child.cancelLongPress();
        }
    }

    @Override
    protected void setChildrenDrawingCacheEnabled(boolean enabled) {
        final int count = getChildCount();
        for (int i = 0; i < count; i++) {
            final View view = getChildAt(i);
            view.setDrawingCacheEnabled(enabled);
            // Update the drawing caches
            if (!view.isHardwareAccelerated() && enabled) {
                view.buildDrawingCache(true);
            }
        }
    }

    protected void setChildrenDrawnWithCacheEnabled(boolean enabled) {
        super.setChildrenDrawnWithCacheEnabled(enabled);
    }

    @Override
    public void setLayerType(int layerType, Paint paint) {
        // When clip children is disabled do not use hardware layer,
        // as hardware layer forces clip children.
        super.setLayerType(getClipChildren() ? layerType : LAYER_TYPE_NONE, paint);
    }
}
