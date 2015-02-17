/*******************************************************************************
 * Copyright (c) 2001, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Mariot Chauvin <mariot.chauvin@obeo.fr> - bug 259553
 *     Amit Joglekar <joglekar@us.ibm.com> - Support for dynamic images (bug 385795)
 *
 * Copyright (C) 2010-2014 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.jkiss.dbeaver.ui.controls.vtabs;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.source.ISharedTextColors;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.UIUtils;


/**
 * Shows the list of tabs in the tabbed property sheet page.
 *
 * @author Anthony Hunter
 * @author Serge Rieder
 */
public class TabbedPropertyList extends Composite {

    private static final ListElement[] ELEMENTS_EMPTY = new ListElement[0];

    protected static final int NONE = -1;

    protected static final int INDENT = 7;
    public static final String LABEL_NA = "N/A";

    private boolean focus = false;

    private ListElement[] elements;

    private int selectedElementIndex = NONE;
    private int topVisibleIndex = NONE;
    private int bottomVisibleIndex = NONE;

    private TopNavigationElement topNavigationElement;
    private BottomNavigationElement bottomNavigationElement;

    private int widestLabelIndex = NONE;
    private int tabsThatFitInComposite = NONE;

    private Color widgetForeground;
    private Color widgetBackground;
    private Color widgetNormalShadow;
    private Color widgetDarkShadow;
    private Color listBackground;
    private Color hoverGradientStart;
    private Color hoverGradientEnd;
    private Color defaultGradientStart;
    private Color defaultGradientEnd;
    private Color indentedDefaultBackground;
    private Color indentedHoverBackground;
    private Color navigationElementShadowStroke;
    private Color bottomNavigationElementShadowStroke1;
    private Color bottomNavigationElementShadowStroke2;

    /**
     * One of the tabs in the tabbed property list.
     */
    public class ListElement extends Canvas {

        private ITabItem tab;
        private int index;
        private boolean selected;
        private boolean hover;

        /**
         * Constructor for ListElement.
         *
         * @param parent the parent Composite.
         * @param tab    the tab item for the element.
         * @param index  the index in the list.
         */
        public ListElement(Composite parent, final ITabItem tab, int index) {
            super(parent, SWT.NO_FOCUS);
            this.tab = tab;
            hover = false;
            selected = false;
            this.index = index;

            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (!selected) {
                        select(getIndex(ListElement.this));
                        /*
						 * We set focus to the tabbed property composite so that
						 * focus is moved to the appropriate widget in the
						 * section.
						 */
                        Composite tabbedPropertyComposite = getParent();
                        tabbedPropertyComposite.setFocus();
                    }
                }
            });
            addMouseMoveListener(new MouseMoveListener() {

                public void mouseMove(MouseEvent e) {
                    if (!hover) {
                        hover = true;
                        redraw();
                    }
                }
            });
            addMouseTrackListener(new MouseTrackAdapter() {

                public void mouseExit(MouseEvent e) {
                    hover = false;
                    redraw();
                }
            });
        }

        /**
         * Set selected value for this element.
         *
         * @param selected the selected value.
         */
        public void setSelected(boolean selected) {
            this.selected = selected;
            redraw();
        }


        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
			/*
			 * draw the top two lines of the tab, same for selected, hover and
			 * default
			 */
            Rectangle bounds = getBounds();
            e.gc.setForeground(widgetNormalShadow);
            e.gc.drawLine(0, 0, bounds.width - 1, 0);
            e.gc.setForeground(listBackground);
            e.gc.drawLine(0, 1, bounds.width - 1, 1);

			/* draw the fill in the tab */
            if (selected) {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 2, bounds.width, bounds.height - 1);
            } else if (hover && tab.isIndented()) {
                e.gc.setBackground(indentedHoverBackground);
                e.gc.fillRectangle(0, 2, bounds.width - 1, bounds.height - 1);
            } else if (hover) {
                e.gc.setForeground(hoverGradientStart);
                e.gc.setBackground(hoverGradientEnd);
                e.gc.fillGradientRectangle(0, 2, bounds.width - 1, bounds.height - 1, true);
            } else if (tab.isIndented()) {
                e.gc.setBackground(indentedDefaultBackground);
                e.gc.fillRectangle(0, 2, bounds.width - 1, bounds.height - 1);
            } else {
                e.gc.setForeground(defaultGradientStart);
                e.gc.setBackground(defaultGradientEnd);
                e.gc.fillGradientRectangle(0, 2, bounds.width - 1, bounds.height - 1, true);
            }

            if (!selected) {
                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(bounds.width - 1, 1, bounds.width - 1,
                    bounds.height + 1);
            }

			/*
			 * Add INDENT pixels to the left as a margin.
			 */
            int textIndent = INDENT;
            FontMetrics fm = e.gc.getFontMetrics();
            int height = fm.getHeight();
            int textMiddle = (bounds.height - height) / 2;

            if (selected && tab.getImage() != null
                && !tab.getImage().isDisposed()) {
				/* draw the icon for the selected tab */
                if (tab.isIndented()) {
                    textIndent = textIndent + INDENT;
                } else {
                    textIndent = textIndent - 3;
                }
                e.gc.drawImage(tab.getImage(), textIndent, textMiddle - 1);
                textIndent = textIndent + 16 + 4;
            } else if (tab.isIndented()) {
                textIndent = textIndent + INDENT;
            }

			/* draw the text */
            e.gc.setForeground(widgetForeground);
            if (selected) {
				/* selected tab is bold font */
                e.gc.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DEFAULT_FONT));
            }
            e.gc.drawText(tab.getText(), textIndent, textMiddle, true);
            if (((TabbedPropertyList) getParent()).focus && selected) {
				/* draw a line if the tab has focus */
                Point point = e.gc.textExtent(tab.getText());
                e.gc.drawLine(textIndent, bounds.height - 4, textIndent + point.x, bounds.height - 4);
            }

			/* draw the bottom line on the tab for selected and default */
            if (!hover) {
                e.gc.setForeground(listBackground);
                e.gc.drawLine(0, bounds.height - 1, bounds.width - 2, bounds.height - 1);
            }
        }

        /**
         * Get the tab item.
         *
         * @return the tab item.
         */
        public ITabItem getTabItem() {
            return tab;
        }

        public String toString() {
            return tab.getText();
        }
    }

    /**
     * The top navigation element in the tabbed property list. It looks like a
     * scroll button when scrolling is needed or is just a spacer when no
     * scrolling is required.
     */
    public class TopNavigationElement extends Canvas {

        /**
         * Constructor for TopNavigationElement.
         *
         * @param parent the parent Composite.
         */
        public TopNavigationElement(Composite parent) {
            super(parent, SWT.NO_FOCUS);
            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (isUpScrollRequired()) {
                        bottomVisibleIndex--;
                        if (topVisibleIndex != 0) {
                            topVisibleIndex--;
                        }
                        layoutTabs();
                        topNavigationElement.redraw();
                        bottomNavigationElement.redraw();
                    }
                }
            });
        }

        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
            e.gc.setBackground(widgetBackground);
            e.gc.setForeground(widgetForeground);
            Rectangle bounds = getBounds();

            if (elements.length != 0) {
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(bounds.width - 1, 0, bounds.width - 1,
                    bounds.height - 1);
            } else {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                int textIndent = INDENT;
                FontMetrics fm = e.gc.getFontMetrics();
                int height = fm.getHeight();
                int textMiddle = (bounds.height - height) / 2;
                e.gc.setForeground(widgetForeground);
                e.gc.drawText(LABEL_NA, textIndent, textMiddle);
            }

            if (isUpScrollRequired()) {
                e.gc.setForeground(widgetDarkShadow);
                int middle = bounds.width / 2;
                e.gc.drawLine(middle + 1, 3, middle + 5, 7);
                e.gc.drawLine(middle, 3, middle - 4, 7);
                e.gc.drawLine(middle - 3, 7, middle + 4, 7);

                e.gc.setForeground(listBackground);
                e.gc.drawLine(middle, 4, middle + 1, 4);
                e.gc.drawLine(middle - 1, 5, middle + 2, 5);
                e.gc.drawLine(middle - 2, 6, middle + 3, 6);

                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(0, 0, bounds.width - 2, 0);
                e.gc.setForeground(navigationElementShadowStroke);
                e.gc.drawLine(0, 1, bounds.width - 2, 1);
                e.gc.drawLine(0, bounds.height - 1, bounds.width - 2,
                    bounds.height - 1);
            }
        }
    }

    /**
     * The top navigation element in the tabbed property list. It looks like a
     * scroll button when scrolling is needed or is just a spacer when no
     * scrolling is required.
     */
    public class BottomNavigationElement extends Canvas {

        /**
         * Constructor for BottomNavigationElement.
         *
         * @param parent the parent Composite.
         */
        public BottomNavigationElement(Composite parent) {
            super(parent, SWT.NO_FOCUS);
            addPaintListener(new PaintListener() {

                public void paintControl(PaintEvent e) {
                    paint(e);
                }
            });
            addMouseListener(new MouseAdapter() {

                public void mouseUp(MouseEvent e) {
                    if (isDownScrollRequired()) {
                        topVisibleIndex++;
                        if (bottomVisibleIndex != elements.length - 1) {
                            bottomVisibleIndex++;
                        }
                        layoutTabs();
                        topNavigationElement.redraw();
                        bottomNavigationElement.redraw();
                    }
                }
            });
        }

        /**
         * Paint the element.
         *
         * @param e the paint event.
         */
        private void paint(PaintEvent e) {
            e.gc.setBackground(widgetBackground);
            e.gc.setForeground(widgetForeground);
            Rectangle bounds = getBounds();

            if (elements.length != 0) {
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(bounds.width - 1, 0, bounds.width - 1,
                    bounds.height - 1);
                e.gc.drawLine(0, 0, bounds.width - 1, 0);

                e.gc.setForeground(bottomNavigationElementShadowStroke1);
                e.gc.drawLine(0, 1, bounds.width - 2, 1);
                e.gc.setForeground(bottomNavigationElementShadowStroke2);
                e.gc.drawLine(0, 2, bounds.width - 2, 2);
            } else {
                e.gc.setBackground(listBackground);
                e.gc.fillRectangle(0, 0, bounds.width, bounds.height);
            }

            if (isDownScrollRequired()) {
                e.gc.setForeground(widgetDarkShadow);
                int middle = bounds.width / 2;
                int bottom = bounds.height - 3;
                e.gc.drawLine(middle + 1, bottom, middle + 5, bottom - 4);
                e.gc.drawLine(middle, bottom, middle - 4, bottom - 4);
                e.gc.drawLine(middle - 3, bottom - 4, middle + 4, bottom - 4);

                e.gc.setForeground(listBackground);
                e.gc.drawLine(middle, bottom - 1, middle + 1, bottom - 1);
                e.gc.drawLine(middle - 1, bottom - 2, middle + 2, bottom - 2);
                e.gc.drawLine(middle - 2, bottom - 3, middle + 3, bottom - 3);

                e.gc.setForeground(widgetNormalShadow);
                e.gc.drawLine(0, bottom - 7, bounds.width - 2, bottom - 7);
                e.gc.setForeground(navigationElementShadowStroke);
                e.gc.drawLine(0, bottom + 2, bounds.width - 2, bottom + 2);
                e.gc.drawLine(0, bottom - 6, bounds.width - 2, bottom - 6);
            }
        }
    }

    public TabbedPropertyList(Composite parent) {
        super(parent, SWT.NO_FOCUS);
        removeAll();
        setLayout(new FormLayout());
        initColours();
        initAccessible();
        topNavigationElement = new TopNavigationElement(this);
        bottomNavigationElement = new BottomNavigationElement(this);

        this.addFocusListener(new FocusListener() {

            public void focusGained(FocusEvent e) {
                focus = true;
                int i = getSelectionIndex();
                if (i >= 0) {
                    elements[i].redraw();
                }
            }

            public void focusLost(FocusEvent e) {
                focus = false;
                int i = getSelectionIndex();
                if (i >= 0) {
                    elements[i].redraw();
                }
            }
        });
        this.addControlListener(new ControlAdapter() {

            public void controlResized(ControlEvent e) {
                computeTopAndBottomTab();
            }
        });
        this.addTraverseListener(new TraverseListener() {

            public void keyTraversed(TraverseEvent e) {
                if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS
                    || e.detail == SWT.TRAVERSE_ARROW_NEXT) {
                    int nMax = elements.length - 1;
                    int nCurrent = getSelectionIndex();
                    if (e.detail == SWT.TRAVERSE_ARROW_PREVIOUS) {
                        nCurrent -= 1;
                        nCurrent = Math.max(0, nCurrent);
                    } else {
                        nCurrent += 1;
                        nCurrent = Math.min(nCurrent, nMax);
                    }
                    select(nCurrent);
                    redraw();
                } else {
                    e.doit = true;
                }
            }
        });
    }

    /**
     * Calculate the number of tabs that will fit in the tab list composite.
     */
    protected void computeTabsThatFitInComposite() {
        tabsThatFitInComposite = Math
            .round((getSize().y - 22) / getTabHeight());
        if (tabsThatFitInComposite <= 0) {
            tabsThatFitInComposite = 1;
        }
    }

    /**
     * Returns the number of elements in this list viewer.
     *
     * @return number of elements
     */
    public int getNumberOfElements() {
        return elements.length;
    }

    /**
     * Returns the element with the given index from this list viewer. Returns
     * <code>null</code> if the index is out of range.
     *
     * @param index the zero-based index
     * @return the element at the given index, or <code>null</code> if the
     * index is out of range
     */
    public ListElement getElementAt(int index) {
        if (index >= 0 && index < elements.length) {
            return elements[index];
        }
        return null;
    }

    /**
     * Returns the zero-relative index of the item which is currently selected
     * in the receiver, or -1 if no item is selected.
     *
     * @return the index of the selected item
     */
    public int getSelectionIndex() {
        return selectedElementIndex;
    }

    /**
     * Removes all elements from this list.
     */
    public void removeAll() {
        if (elements != null) {
            for (ListElement element : elements) {
                element.dispose();
            }
        }
        elements = ELEMENTS_EMPTY;
        selectedElementIndex = NONE;
        widestLabelIndex = NONE;
        topVisibleIndex = NONE;
        bottomVisibleIndex = NONE;
    }

    /**
     * Sets the new list elements.
     */
    public void setElements(ITabItem[] children) {
        if (elements != ELEMENTS_EMPTY) {
            removeAll();
        }
        elements = new ListElement[children.length];
        if (children.length == 0) {
            widestLabelIndex = NONE;
        } else {
            widestLabelIndex = 0;
            for (int i = 0; i < children.length; i++) {
                elements[i] = new ListElement(this, children[i], i);
                elements[i].setVisible(false);
                elements[i].setLayoutData(null);

                if (i != widestLabelIndex) {
                    int width = getTabWidth(children[i]);
                    if (width > getTabWidth(children[widestLabelIndex])) {
                        widestLabelIndex = i;
                    }
                }
            }
        }

        computeTopAndBottomTab();
    }

    private int getTabWidth(ITabItem tabItem) {
        int width = getTextDimension(tabItem.getText()).x;
		/*
		 * To anticipate for the icon placement we should always keep the
		 * space available after the label. So when the active tab includes
		 * an icon the width of the tab doesn't change.
		 */
        if (tabItem.getImage() != null) {
            width = width + tabItem.getImage().getBounds().x + 4;
        }
        if (tabItem.isIndented()) {
            width = width + INDENT;
        }
        return width;
    }

    /**
     * Selects one of the elements in the list.
     *
     * @param index the index of the element to select.
     */
    protected void select(int index) {
        if (getSelectionIndex() == index) {
			/*
			 * this index is already selected.
			 */
            return;
        }
        if (index >= 0 && index < elements.length) {
            int lastSelected = getSelectionIndex();
            elements[index].setSelected(true);
            selectedElementIndex = index;
            if (lastSelected != NONE) {
                elements[lastSelected].setSelected(false);
                if (getSelectionIndex() != elements.length - 1) {
					/*
					 * redraw the next tab to fix the border by calling
					 * setSelected()
					 */
                    elements[getSelectionIndex() + 1].setSelected(false);
                }
            }
            topNavigationElement.redraw();
            bottomNavigationElement.redraw();

            if (selectedElementIndex < topVisibleIndex
                || selectedElementIndex > bottomVisibleIndex) {
                computeTopAndBottomTab();
            }
        }
        notifyListeners(SWT.Selection, new Event());
    }

    /**
     * Deselects all the elements in the list.
     */
    public void deselectAll() {
        if (getSelectionIndex() != NONE) {
            elements[getSelectionIndex()].setSelected(false);
            selectedElementIndex = NONE;
        }
    }

    private int getIndex(ListElement element) {
        return element.index;
    }

    public Point computeSize(int wHint, int hHint, boolean changed) {
        Point result = super.computeSize(hHint, wHint, changed);
        if (widestLabelIndex == -1) {
            result.x = getTextDimension(LABEL_NA).x + INDENT;
        } else {
			/*
			 * Add INDENT pixels to the left of the longest tab as a margin.
			 */
            int width = getTabWidth(elements[widestLabelIndex].getTabItem()) + INDENT;
			/*
			 * Add 10 pixels to the right of the longest tab as a margin.
			 */
            result.x = width + 10;
        }
        return result;
    }

    /**
     * Get the dimensions of the provided string.
     *
     * @param text the string.
     * @return the dimensions of the provided string.
     */
    private Point getTextDimension(String text) {
        GC gc = new GC(this);
        gc.setFont(JFaceResources.getFontRegistry().getBold(
            JFaceResources.DEFAULT_FONT));
        Point point = gc.textExtent(text);
        point.x++;
        gc.dispose();
        return point;
    }

    /**
     * Initialize the colours used in the list.
     */
    private void initColours() {
        Display display = Display.getCurrent();
        ISharedTextColors sharedColors = DBeaverUI.getSharedTextColors();

        listBackground = display.getSystemColor(SWT.COLOR_LIST_BACKGROUND);
        widgetBackground = display.getSystemColor(SWT.COLOR_WIDGET_BACKGROUND);
        widgetDarkShadow = display.getSystemColor(SWT.COLOR_WIDGET_DARK_SHADOW);
        widgetForeground = display.getSystemColor(SWT.COLOR_WIDGET_FOREGROUND);
        widgetNormalShadow = display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);

        RGB infoBackground = display.getSystemColor(SWT.COLOR_INFO_BACKGROUND).getRGB();
        RGB white = display.getSystemColor(SWT.COLOR_WHITE).getRGB();
        RGB black = display.getSystemColor(SWT.COLOR_BLACK).getRGB();

		/*
		 * gradient in the default tab: start colour WIDGET_NORMAL_SHADOW 100% +
		 * white 20% + INFO_BACKGROUND 60% end colour WIDGET_NORMAL_SHADOW 100% +
		 * INFO_BACKGROUND 40%
		 */
        defaultGradientStart = sharedColors.getColor(
            UIUtils.blend(infoBackground,
                UIUtils.blend(white, widgetNormalShadow.getRGB(), 20), 60)
        );
        defaultGradientEnd = sharedColors.getColor(UIUtils.blend(infoBackground, widgetNormalShadow.getRGB(), 40));

        navigationElementShadowStroke = sharedColors.getColor(UIUtils.blend(white, widgetNormalShadow.getRGB(), 55));
        bottomNavigationElementShadowStroke1 = sharedColors.getColor(UIUtils.blend(black, widgetBackground.getRGB(), 10));
        bottomNavigationElementShadowStroke2 = sharedColors.getColor(UIUtils.blend(black, widgetBackground.getRGB(), 5));

		/*
		 * gradient in the hover tab: start colour WIDGET_BACKGROUND 100% +
		 * white 20% end colour WIDGET_BACKGROUND 100% + WIDGET_NORMAL_SHADOW
		 * 10%
		 */
        hoverGradientStart = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 20));
        hoverGradientEnd = sharedColors.getColor(UIUtils.blend(widgetNormalShadow.getRGB(), widgetBackground.getRGB(), 10));

        indentedDefaultBackground = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 10));
        indentedHoverBackground = sharedColors.getColor(UIUtils.blend(white, widgetBackground.getRGB(), 75));
    }

    /**
     * Get the height of a tab. The height of the tab is the height of the text
     * plus buffer.
     *
     * @return the height of a tab.
     */
    private int getTabHeight() {
        int tabHeight = getTextDimension("").y + INDENT; //$NON-NLS-1$
        if (tabsThatFitInComposite == 1) {
			/*
			 * if only one tab will fix, reduce the size of the tab height so
			 * that the navigation elements fit.
			 */
            int ret = getBounds().height - 20;
            return (ret > tabHeight) ? tabHeight
                : (ret < 5) ? 5
                : ret;
        }
        return tabHeight;
    }

    /**
     * Determine if a downward scrolling is required.
     *
     * @return true if downward scrolling is required.
     */
    private boolean isDownScrollRequired() {
        return elements.length > tabsThatFitInComposite
            && bottomVisibleIndex != elements.length - 1;
    }

    /**
     * Determine if an upward scrolling is required.
     *
     * @return true if upward scrolling is required.
     */
    private boolean isUpScrollRequired() {
        return elements.length > tabsThatFitInComposite && topVisibleIndex != 0;
    }

    /**
     * Based on available space, figure out the top and bottom tabs in the list.
     */
    private void computeTopAndBottomTab() {
        computeTabsThatFitInComposite();
        if (elements.length == 0) {
			/*
			 * no tabs to display.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = 0;
        } else if (tabsThatFitInComposite >= elements.length) {
			/*
			 * all the tabs fit.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = elements.length - 1;
        } else if (getSelectionIndex() == NONE) {
			/*
			 * there is no selected tab yet, assume that tab one would
			 * be selected for now.
			 */
            topVisibleIndex = 0;
            bottomVisibleIndex = tabsThatFitInComposite - 1;
        } else if (getSelectionIndex() + tabsThatFitInComposite > elements.length) {
			/*
			 * the selected tab is near the bottom.
			 */
            bottomVisibleIndex = elements.length - 1;
            topVisibleIndex = bottomVisibleIndex - tabsThatFitInComposite + 1;
        } else {
			/*
			 * the selected tab is near the top.
			 */
            topVisibleIndex = selectedElementIndex;
            bottomVisibleIndex = selectedElementIndex + tabsThatFitInComposite
                - 1;
        }
        layoutTabs();
    }

    /**
     * Layout the tabs.
     */
    private void layoutTabs() {
        if (tabsThatFitInComposite == NONE || elements.length == 0) {
            FormData formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(0, 0);
            formData.height = getTabHeight();
            topNavigationElement.setLayoutData(formData);

            formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(topNavigationElement, 0);
            formData.bottom = new FormAttachment(100, 0);
            bottomNavigationElement.setLayoutData(formData);
        } else {

            FormData formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(0, 0);
            formData.height = 10;
            topNavigationElement.setLayoutData(formData);

			/*
			 * use nextElement to attach the layout to the previous canvas
			 * widget in the list.
			 */
            Canvas nextElement = topNavigationElement;

            for (int i = 0; i < elements.length; i++) {
                //System.out.print(i + " [" + elements[i].getText() + "]");
                if (i < topVisibleIndex || i > bottomVisibleIndex) {
					/*
					 * this tab is not visible
					 */
                    elements[i].setLayoutData(null);
                    elements[i].setVisible(false);
                } else {
					/*
					 * this tab is visible.
					 */
                    //System.out.print(" visible");
                    formData = new FormData();
                    formData.height = getTabHeight();
                    formData.left = new FormAttachment(0, 0);
                    formData.right = new FormAttachment(100, 0);
                    formData.top = new FormAttachment(nextElement, 0);
                    nextElement = elements[i];
                    elements[i].setLayoutData(formData);
                    elements[i].setVisible(true);
                }

                //if (i == selectedElementIndex) {
                //	System.out.print(" selected");
                //}
                //System.out.println("");
            }
            formData = new FormData();
            formData.left = new FormAttachment(0, 0);
            formData.right = new FormAttachment(100, 0);
            formData.top = new FormAttachment(nextElement, 0);
            formData.bottom = new FormAttachment(100, 0);
            formData.height = 10;
            bottomNavigationElement.setLayoutData(formData);
        }
        //System.out.println("");

        // layout so that we have enough space for the new labels
        Composite grandparent = getParent().getParent();
        grandparent.layout(true);
        layout(true);
    }

    /**
     * Initialize the accessibility adapter.
     */
    private void initAccessible() {
        final Accessible accessible = getAccessible();
        accessible.addAccessibleListener(new AccessibleAdapter() {

            public void getName(AccessibleEvent e) {
                if (getSelectionIndex() != NONE) {
                    e.result = elements[getSelectionIndex()].getTabItem().getText();
                }
            }

            public void getHelp(AccessibleEvent e) {
                if (getSelectionIndex() != NONE) {
                    e.result = elements[getSelectionIndex()].getTabItem().getText();
                }
            }
        });

        accessible.addAccessibleControlListener(new AccessibleControlAdapter() {

            public void getChildAtPoint(AccessibleControlEvent e) {
                Point pt = toControl(new Point(e.x, e.y));
                e.childID = (getBounds().contains(pt)) ? ACC.CHILDID_SELF : ACC.CHILDID_NONE;
            }

            public void getLocation(AccessibleControlEvent e) {
                if (getSelectionIndex() != NONE) {
                    Rectangle location = elements[getSelectionIndex()].getBounds();
                    Point pt = toDisplay(new Point(location.x, location.y));
                    e.x = pt.x;
                    e.y = pt.y;
                    e.width = location.width;
                    e.height = location.height;
                }
            }

            public void getChildCount(AccessibleControlEvent e) {
                e.detail = 0;
            }

            public void getRole(AccessibleControlEvent e) {
                e.detail = ACC.ROLE_TABITEM;
            }

            public void getState(AccessibleControlEvent e) {
                e.detail = ACC.STATE_SELECTABLE | ACC.STATE_SELECTED | ACC.STATE_FOCUSED | ACC.STATE_FOCUSABLE;
            }
        });

        addListener(SWT.Selection, new Listener() {

            public void handleEvent(Event event) {
                if (isFocusControl()) {
                    accessible.setFocus(ACC.CHILDID_SELF);
                }
            }
        });

        addListener(SWT.FocusIn, new Listener() {

            public void handleEvent(Event event) {
                accessible.setFocus(ACC.CHILDID_SELF);
            }
        });
    }
}