/* Pdfviewer.java

	Purpose:

	Description:

	History:
		Mon Oct 14 18:45:02 CST 2019, Created by rudyhuang

Copyright (C) 2019 Potix Corporation. All Rights Reserved.
*/
package org.zkoss.zkex.zul;

import java.util.Map;
import org.zkoss.lang.Objects;
import org.zkoss.util.media.Media;
import org.zkoss.zk.au.AuRequest;
import org.zkoss.zk.au.AuRequests;
import org.zkoss.zk.au.DeferredValue;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.UiException;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.ext.render.DynamicMedia;
import org.zkoss.zkex.ui.event.RotationEvent;
import org.zkoss.zkex.ui.event.ZoomEvent;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.event.PagingEvent;
import org.zkoss.zul.event.ZulEvents;
import org.zkoss.zul.ext.Pageable;
import org.zkoss.zul.impl.Utils;
import org.zkoss.zul.impl.XulElement;

/**
 * A PDF file viewer.
 * <p>Only works for browsers supporting HTML5 (Firefox, Chrome, Opera, IE 11, Edge, Safari...).
 * <p>Available in ZK PE and ZK EE.
 * <p>Default {@link #getZclass}: z-pdfviewer.
 *
 * @author rudyhuang
 * @since 9.0.0
 */
public class Pdfviewer extends XulElement implements Pageable {
    static {
        addClientEvent(Pdfviewer.class, ZulEvents.ON_PAGING, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
        addClientEvent(Pdfviewer.class, Events.ON_RENDER, CE_IMPORTANT);
        addClientEvent(Pdfviewer.class, org.zkoss.zkex.ui.event.Events.ON_ZOOM, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
        addClientEvent(Pdfviewer.class, org.zkoss.zkex.ui.event.Events.ON_ROTATE, CE_IMPORTANT | CE_DUPLICATE_IGNORE);
    }

    private String _src;
    private Media _content;
    private byte _contentVersion;
    private int _pageCount = 1;
    private int _activePage;
    private double _zoom = 1.0;
    private int _rotation;
    private Toolbar _toolbar;

    /**
     * Returns the source URI of the PDF file.
     * <p>Default: null.
     */
    public String getSrc() {
        return _src;
    }

    /**
     * Sets the source URI of the PDF file.
     *
     * <p>Calling this method implies setContent(null).
     * In other words, the last invocation of setSrc overrides
     * the previous {@link #setContent}, if any.
     */
    public void setSrc(String src) {
        if (!Objects.equals(_src, src)) {
            _src = src;
            _content = null;
            _activePage = 0;
            smartUpdate("src", _src);
        }
    }

    /**
     * Returns the content set by {@link #setContent}.
     * <p>Default: null.
     * <p>Note: it won't fetch what is set thru by {@link #setSrc}.
     * It simply returns what is passed to {@link #setContent}.
     */
    public Media getContent() {
        return _content;
    }

    /**
     * Sets the content.
     *
     * <p>Calling this method implies setSrc(null).
     * In other words, the last invocation of setContent overrides
     * the previous {@link #setSrc}, if any.
     */
    public void setContent(Media content) {
        if (_src != null || content != _content) {
            _content = content;
            _src = null;
            if (_content != null)
                ++_contentVersion; // enforce browser to reload
            _activePage = 0;
            _src = this.getEncodedSrc();
            smartUpdate("src", (DeferredValue) this::getEncodedSrc);
        }
    }

    private String getEncodedSrc() {
        final Desktop dt = getDesktop();
        if (_content != null) {
            return Utils.getDynamicMediaURI(this, _contentVersion, _content.getName(), _content.getFormat());
        } else if (dt != null) {
            return dt.getExecution().encodeURL(_src);
        }
        return null;
    }

    @Override
    public int getActivePage() {
        return _activePage;
    }

    @Override
    public void setActivePage(int page) throws WrongValueException {
        if (page < 0)
            throw new WrongValueException("Page cannot be negative: " + page);

        if (_activePage != page) {
            _activePage = page;
            smartUpdate("activePage", _activePage);
        }
    }

    /**
     * Returns the zoom level.
     * <p>Default: 1.0.
     */
    public double getZoom() {
        return _zoom;
    }

    /**
     * Sets the zoom level.
     */
    public void setZoom(double zoom) {
        if (zoom <= 0)
            throw new WrongValueException("Zoom should be positive: " + zoom);
        if (_zoom != zoom) {
            _zoom = zoom;
            smartUpdate("zoom", _zoom);
        }
    }

    /**
     * Returns the rotation angle.
     * <p>Default: 0.
     */
    public int getRotation() {
        return _rotation;
    }

    /**
     * Sets the rotation angle.
     * @param rotation rotation angle. Only 0, 90, 180 and 270 are accepted.
     */
    public void setRotation(int rotation) {
        if (rotation < 0 || rotation >= 360)
            throw new WrongValueException("invalid degrees: " + rotation);
        if (rotation % 90 != 0)
            throw new WrongValueException("support multiple of 90 degrees only: " + rotation);
        if (_rotation != rotation) {
            _rotation = rotation;
            smartUpdate("rotation", _rotation);
        }
    }

    @Override
    protected void renderProperties(org.zkoss.zk.ui.sys.ContentRenderer renderer)
            throws java.io.IOException {
        org.zkoss.zkex.rt.Runtime.init(this);
        super.renderProperties(renderer);

        render(renderer, "src", _src);
        if (_activePage != 0)
            render(renderer, "activePage", _activePage);
        if (_zoom != 1.0)
            render(renderer, "zoom", _zoom);
        if (_rotation != 0)
            render(renderer, "rotation", _rotation);
    }

    /**
     * Returns the number of items per page.
     * <p>Default: 1.
     */
    @Override
    public int getPageSize() {
        return 1;
    }

    /**
     * Sets the number of items per page.
     * <p>It is readonly in pdfviewer.
     */
    @Override
    public void setPageSize(int size) throws WrongValueException {
        throw new UnsupportedOperationException("readonly");
    }

    @Override
    public int getPageCount() {
        return _pageCount;
    }

    /**
     * Go to the first page.
     * @return {@code true} if the action was successful.
     */
    public boolean firstPage() {
        setActivePage(0);
        return true;
    }

    /**
     * Go to the previous page.
     * @return {@code true} if the action was successful.
     */
    public boolean previousPage() {
        int targetPage = _activePage - 1;
        if (targetPage >= 0) {
            setActivePage(targetPage);
            return true;
        }
        return false;
    }

    /**
     * Go to the next page.
     * @return {@code true} if the action was successful.
     */
    public boolean nextPage() {
        int targetPage = _activePage + 1;
        if (targetPage < _pageCount) {
            setActivePage(targetPage);
            return true;
        }
        return false;
    }

    /**
     * Go to the last page.
     * @return {@code true} if the action was successful.
     */
    public boolean lastPage() {
        setActivePage(getPageCount() - 1);
        return true;
    }

    /**
     * Zoom in by 10%.
     * @return {@code true} if the action was successful.
     */
    public boolean zoomIn() {
        setZoom(_zoom + 0.1);
        return true;
    }

    /**
     * Zoom out by 10%.
     * @return {@code true} if the action was successful.
     */
    public boolean zoomOut() {
        double newValue = _zoom - 0.1;
        if (newValue > 0) {
            setZoom(newValue);
            return true;
        }
        return false;
    }

    /**
     * Rotates 90 degrees clockwise.
     */
    public void rotateClockwise() {
        setRotation((_rotation + 90) % 360);
    }

    /**
     * Rotates 90 degrees counterclockwise.
     */
    public void rotateCounterclockwise() {
        int newValue = _rotation - 90;
        if (newValue < 0)
            newValue = 270;
        setRotation(newValue);
    }

    @Override
    public void service(AuRequest request, boolean everError) {
        final String cmd = request.getCommand();
        final Map<String, Object> data = request.getData();

        switch (cmd) {
            case Events.ON_RENDER:
                _pageCount = AuRequests.getInt(data, "pageCount", 0);
                Events.postEvent(Event.getEvent(request));
                break;
            case ZulEvents.ON_PAGING:
                final PagingEvent pagingEvent = PagingEvent.getPagingEvent(request);
                _activePage = pagingEvent.getActivePage();
                Events.postEvent(pagingEvent);
                break;
            case org.zkoss.zkex.ui.event.Events.ON_ZOOM:
                final ZoomEvent zoomEvent = ZoomEvent.getZoomEvent(request);
                _zoom = zoomEvent.getZoom();
                Events.postEvent(zoomEvent);
                break;
            case org.zkoss.zkex.ui.event.Events.ON_ROTATE:
                final RotationEvent rotationEvent = RotationEvent.getRotationEvent(request);
                _rotation = rotationEvent.getRotation();
                Events.postEvent(rotationEvent);
                break;
            default:
                super.service(request, everError);
                break;
        }
    }

    @Override
    public void beforeChildAdded(Component child, Component insertBefore) {
        if (!(child instanceof Toolbar))
            throw new UiException("Unsupported child: " + child);
        if (_toolbar != null)
            throw new UiException("Only one toolbar is allowed: " + this);
        super.beforeChildAdded(child, insertBefore);
    }

    @Override
    public void onChildAdded(Component child) {
        super.onChildAdded(child);
        if (child instanceof Toolbar) {
            _toolbar = (Toolbar) child;
        }
    }

    @Override
    public void onChildRemoved(Component child) {
        if (_toolbar == child) {
            _toolbar = null;
        }
        super.onChildRemoved(child);
    }


    @Override
    public String getZclass() {
        return (this._zclass != null ? this._zclass : "z-pdfviewer");
    }

    @Override
    public Object getExtraCtrl() {
        return new ExtraCtrl();
    }

    /**
     * A utility class to implement {@link #getExtraCtrl}.
     * It is used only by component developers.
     */
    protected class ExtraCtrl extends XulElement.ExtraCtrl implements DynamicMedia {
        @Override
        public Media getMedia(String pathInfo) {
            return _content;
        }
    }
}

