/*  Copyright (c) 2008 Konrad-Zuse-Zentrum fuer Informationstechnik Berlin.

    This file is part of XtreemFS. XtreemFS is part of XtreemOS, a Linux-based
    Grid Operating System, see <http://www.xtreemos.eu> for more details.
    The XtreemOS project has been developed with the financial support of the
    European Commission's IST program under contract #FP6-033576.

    XtreemFS is free software: you can redistribute it and/or modify it under
    the terms of the GNU General Public License as published by the Free
    Software Foundation, either version 2 of the License, or (at your option)
    any later version.

    XtreemFS is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with XtreemFS. If not, see <http://www.gnu.org/licenses/>.
*/
/*
 * AUTHORS: Jan Stender (ZIB), Björn Kolbeck (ZIB)
 */

package org.xtreemfs.mrc.brain;

/**
 * Parses a path separated by '/' into multiple sections.
 * 
 * @author stender
 * 
 */
public class Path {

    private String volume;

    private String innerPart;

    private String lastPart;

    private String pathWithoutVolume;

    private String path;

    public Path(String volume, String innerPart, String lastPart) {
        this.volume = volume;
        this.innerPart = innerPart;
        this.lastPart = lastPart;

        pathWithoutVolume = innerPart + "/" + lastPart;
        path = volume
                + "/"
                + (innerPart.isEmpty() ? lastPart
                        : (innerPart + "/" + lastPart));
    }

    public Path(String path) {

        this.path = path;

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < path.length() && path.charAt(i) != '/'; i++)
            sb.append(path.charAt(i));

        volume = sb.toString();

        sb = new StringBuffer();
        for (int i = path.length() - 1; i >= volume.length()
                && path.charAt(i) != '/'; i--)
            sb.append(path.charAt(i));

        lastPart = sb.reverse().toString();

        sb = new StringBuffer();
        for (int i = volume.length() + 1; i <= path.length()
                - lastPart.length() - 1; i++) {
            sb.append(path.charAt(i));
        }

        innerPart = sb.toString();

        for (int i = path.length() - lastPart.length(); i < path.length(); i++)
            sb.append(path.charAt(i));

        pathWithoutVolume = sb.toString();
    }

    public String getVolumeName() {
        return volume;
    }

    public String getInnerPath() {
        return innerPart;
    }

    public String getPathWithoutVolume() {
        return pathWithoutVolume;
    }

    public String getLastPathComponent() {
        return lastPart;
    }

    public static void main(String[] args) {
        Path path = new Path("myVolume/test/blub/bla.txt");
        System.out.println(path.getVolumeName() + "\n" + path.getInnerPath()
                + "\n" + path.getLastPathComponent() + "\n"
                + path.getPathWithoutVolume());
    }

    public String toString() {
        return path;
    }

}
