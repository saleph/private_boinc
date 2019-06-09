/*
 * 2010-2017 (C) Antonio Redondo
 * http://antonioredondo.com
 * http://github.com/AntonioRedondo/AnotherMonitor
 *
 * Code under the terms of the GNU General Public License v3.
 *
 */

package edu.berkeley.boinc;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.TextureView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.Math.floor;

public class ViewGraphic extends TextureView {

    private boolean graphicInitialized;
    private int yTop;
    private int yBottom;
    private int xLeft;
    private int xRight;
    private int graphicHeight;
    private int graphicWidth;
    private int minutes;
    private int seconds;
    private int intervalTotalNumber;
    private Long memTotal;
    private int thickParam;
    private int thickGrid;
    private int thickEdges;
    private int textSizeLegend;
    private Rect bgRect;
    private Paint bgPaint;
    private Paint textPaintLegend;
    private Paint textPaintLegendV;
    private Paint linesEdgePaint;
    private Paint linesGridPaint;
    private Paint applicationUsagePaint;
    private Paint memUsedPaint;
    private Paint memAvailablePaint;
    private Paint memFreePaint;
    private Paint cachedPaint;
    private Paint thresholdPaint;
    private Collection<Long> memoryAM;
    private Collection<Long> memUsed;
    private Collection<Long> memAvailable;
    private Collection<Long> memFree;
    private Collection<Long> cached;
    private Collection<Long> threshold;
    private ReaderService readerService;
    private Resources resources;
    private Thread drawThread;
    private Canvas canvas;


    public ViewGraphic(Context context, AttributeSet attrs) {
        super(context, attrs);
        resources = getResources();
        float density = resources.getDisplayMetrics().density;
        thickGrid = (int) Math.ceil(1 * density);
        thickParam = (int) Math.ceil(1 * density);
        thickEdges = (int) Math.ceil(2 * density);
        textSizeLegend = (int) Math.ceil(10 * density);
    }


    // https://groups.google.com/a/chromium.org/forum/#!topic/graphics-dev/Z0yE-PWQXc4
    // http://www.edu4java.com/en/androidgame/androidgame2.html
    protected void onDrawCustomised(Canvas canvasInstance, Thread thread) {
        if (!graphicInitialized) {
            initializeGraphic();
        }
        if (readerService == null || canvasInstance == null || thread == null) {
            return;
        }
        drawThread = thread;
        canvas = canvasInstance;

        drawBackground();
        drawHorizontalGridLines();
        drawVerticalGridLines();
        drawPlot(memoryAM, applicationUsagePaint);
        drawPlot(memUsed, memUsedPaint);
        drawPlot(memAvailable, memAvailablePaint);
        drawPlot(memFree, memFreePaint);
        drawPlot(cached, cachedPaint);
        drawPlot(threshold, thresholdPaint);
        drawHorizontalEdges();
        drawVerticalEdges();
        drawHorizontalLegend();
        drawVerticalLegend();
    }

    private void initializeGraphic() {
        yTop = (int) (getHeight() * 0.1);
        yBottom = (int) (getHeight() * 0.88);
        xLeft = (int) (getWidth() * 0.14);
        xRight = (int) (getWidth() * 0.94);

        graphicWidth = xRight - xLeft;
        graphicHeight = yBottom - yTop;

        bgRect = new Rect(xLeft, yTop, xRight, yBottom);

        calculateInnerVariables();

        bgPaint = getPaint(Color.LTGRAY, Paint.Align.CENTER, 12, false, 0);

        linesEdgePaint = getPaint(resources.getColor(R.color.shadow), Paint.Align.CENTER, 12, false,
                                  thickEdges);
        linesGridPaint = getPaint(resources.getColor(R.color.shadow), Paint.Align.CENTER, 12, false,
                                  thickGrid);
        linesGridPaint.setStyle(Style.STROKE);
        linesGridPaint.setPathEffect(new DashPathEffect(new float[]{8, 8}, 0));

        memUsedPaint = getPaint(resources.getColor(R.color.Orange), Paint.Align.CENTER, 12, false,
                                thickParam);
        applicationUsagePaint = getPaint(Color.YELLOW, Paint.Align.CENTER, 12, false, thickParam);
        memAvailablePaint = getPaint(Color.MAGENTA, Paint.Align.CENTER, 12, false, thickParam);
        memFreePaint = getPaint(Color.parseColor("#804000"), Paint.Align.CENTER, 12, false,
                                thickParam);
        cachedPaint = getPaint(Color.BLUE, Paint.Align.CENTER, 12, false, thickParam);
        thresholdPaint = getPaint(Color.GREEN, Paint.Align.CENTER, 12, false, thickParam);

        textPaintLegend = getPaint(Color.DKGRAY, Paint.Align.CENTER, textSizeLegend, true, 0);
        textPaintLegendV = getPaint(Color.DKGRAY, Paint.Align.RIGHT, textSizeLegend, true, 0);
        graphicInitialized = true;
    }

    private void drawVerticalLegend() {
        int xLeftTextSpace = 10;
        int textXPosition = xLeft - xLeftTextSpace;
        drawText("100%", textXPosition, yTop + 5, textPaintLegendV);
        if (drawThread.isInterrupted())
            return;
        int yLegendSpace = 8;
        drawText("90%", textXPosition, yTop + graphicHeight * 0.1f + yLegendSpace,
                 textPaintLegendV);
        drawText("70%", textXPosition, yTop + graphicHeight * 0.3f + yLegendSpace,
                 textPaintLegendV);
        drawText("50%", textXPosition, yTop + graphicHeight * 0.5f + yLegendSpace,
                 textPaintLegendV);
        drawText("30%", textXPosition, yTop + graphicHeight * 0.7f + yLegendSpace,
                 textPaintLegendV);
        drawText("10%", textXPosition, yTop + graphicHeight * 0.9f + yLegendSpace,
                 textPaintLegendV);
        drawText("0%", textXPosition, yBottom + yLegendSpace, textPaintLegendV);
    }

    private void drawHorizontalLegend() {
        int yBottomTextSpace = 25;
        for (int n = 0; n <= minutes; ++n) {
            drawText(n + "'",
                     (int) floor(xLeft + n * readerService.getIntervalWidthInSeconds() * (int) (60 / ((float) readerService.getIntervalRead() / 1000))), yBottom + yBottomTextSpace, textPaintLegend);
        }
        if (minutes == 0) {
            drawText(seconds + "\"", xLeft, yBottom + yBottomTextSpace, textPaintLegend);
        }
    }

    private void drawVerticalEdges() {
        drawLine(xLeft, yTop, xLeft, yBottom, linesEdgePaint);
        drawLine(xRight, yBottom, xRight, yTop, linesEdgePaint);
    }

    private void drawHorizontalEdges() {
        drawLine(xLeft, yTop, xRight, yTop, linesEdgePaint);
        drawLine(xLeft, yBottom, xRight, yBottom, linesEdgePaint);
    }

    private void drawVerticalGridLines() {
        for (int n = 1; n <= minutes; ++n) {
            int linePositionOnX = (int) floor(xRight - n * readerService.getIntervalWidthInSeconds() * floor(60.0 / (readerService.getIntervalRead() / 1000.0)));
            drawLine(linePositionOnX, yTop, linePositionOnX, yBottom, linesGridPaint);
        }
    }

    private void drawHorizontalGridLines() {
        for (float n = 0.1f; n < 1.0f; n = n + 0.2f) {
            drawLine(xLeft, yTop + graphicHeight * n, xRight, yTop + graphicHeight * n,
                     linesGridPaint);
        }
    }

    private void drawBackground() {
        if (drawThread.isInterrupted())
            return;
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        if (drawThread.isInterrupted())
            return;
        canvas.drawRect(bgRect, bgPaint);
    }


    private void drawPlot(Collection<Long> yColl, Paint paint) {
        List<Long> y = new ArrayList<>(yColl);
        if (y.size() <= 1) {
            return;
        }
        for (int m = 0; m < (y.size() - 1) && m < intervalTotalNumber; ++m) {
            drawLine((int) (xLeft + readerService.getIntervalWidthInSeconds() * m),
                     yBottom - y.get(m) * graphicHeight / memTotal,
                     (int) (xLeft + readerService.getIntervalWidthInSeconds() * m + readerService.getIntervalWidthInSeconds()),
                     yBottom - y.get(m + 1) * graphicHeight / memTotal, paint);
        }
    }


    private Paint getPaint(int color, Paint.Align textAlign, int textSize, boolean antiAlias, float strokeWidth) {
        Paint p = new Paint();
        p.setColor(color);
        p.setTextSize(textSize);
        p.setTextAlign(textAlign);
        p.setAntiAlias(antiAlias);
        p.setStrokeWidth(strokeWidth);
        return p;
    }


    void setService(ReaderService sr) {
        readerService = sr;
        memoryAM = readerService.getMemoryAM();
        memTotal = readerService.getMemTotal();
        memUsed = readerService.getMemUsed();
        memAvailable = readerService.getMemAvailable();
        memFree = readerService.getMemFree();
        cached = readerService.getCached();
        threshold = readerService.getThreshold();
    }

    void calculateInnerVariables() {
        intervalTotalNumber = (int) Math.ceil(graphicWidth / readerService.getIntervalWidthInSeconds());
        minutes = (int) floor(intervalTotalNumber * readerService.getIntervalRead() / 1000.0 / 60.0);
        seconds = (int) floor(intervalTotalNumber * readerService.getIntervalRead() / 1000.0);
    }

    void drawLine(float startX, float startY, float stopX, float stopY, Paint paint) {
        if (drawThread.isInterrupted()) {
            return;
        }
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }

    void drawText(String text, float x, float y, Paint paint) {
        if (drawThread.isInterrupted())
            return;
        canvas.drawText(text, x, y, paint);
    }
}
