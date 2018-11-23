/*
 * Copyright 2018 MovingBlocks
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
package org.destinationsol.ui;


public class DisplayDimensions {
    private int width;
    private int height;
    private float ratio;

    public DisplayDimensions(int width, int height) {
        set(width, height);
    }

    public void set(int width, int height) {
        this.width = width;
        this.height = height;
        ratio = width / (float)height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Should be equal to {@code ((float) gameWindowWidth) / ((float) gameWindowHeight)}.
     * <p>
     * Old {@code r} variable in lots of places.
     */
    public float getRatio() {
        return ratio;
    }

    public float getFloatWidthForPixelWidth(int width) {
        return (float) width * ratio / this.width;
    }

    public float getFloatHeightForPixelHeight(int height) {
        return (float) height / this.height;
    }
}
