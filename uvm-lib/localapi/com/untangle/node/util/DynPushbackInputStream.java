/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc. 
 *
 * This library is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This library is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent modules,
 * and to copy and distribute the resulting executable under terms of your
 * choice, provided that you also meet, for each linked independent module,
 * the terms and conditions of the license of that module.  An independent
 * module is a module which is not derived from or based on this library.
 * If you modify this library, you may extend this exception to your version
 * of the library, but you are not obligated to do so.  If you do not wish
 * to do so, delete this exception statement from your version.
 */

package com.untangle.node.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * A PushbackInputStream which is dynamic (unlike the one from
 * JavaSoft) in that it does not have a fixed internal
 * buffer size.
 */
public class DynPushbackInputStream
    extends FilterInputStream {

    private final ByteStack m_stack;
    private boolean m_closed = false;


    public DynPushbackInputStream(InputStream wrapped) {
        this(wrapped, 1024, 1024);
    }
    public DynPushbackInputStream(InputStream wrapped,
                                  int expandBy) {
        this(wrapped, expandBy, expandBy);
    }

    /**
     * Construct a new DynPushbackInputStream wrapping
     * the given Stream with the initial capacity
     * and expandBy values
     *
     * @param wrapped the stream to wrap
     * @param initCapacity the initial capacity of the
     *        internal pushback buffer
     * @param expandBy the quantity the internal buffer should
     *        be expanded-by if a pushback exceeds the internal
     *        capacity.
     */
    public DynPushbackInputStream(InputStream wrapped,
                                  int initCapacity,
                                  int expandBy) {
        super(wrapped);
        m_stack = new ByteStack(initCapacity, expandBy);
    }


    /**
     * Unread the byte.  The next call to read will
     * return this byte.
     */
    public void unread(int b)
        throws IOException {
        ensureOpen();
        m_stack.push(b);
    }
    /**
     * Unread the bytes.  The next call to read will
     * return b[0].
     */
    public void unread(byte[] b)
        throws IOException {
        unread(b, 0, b.length);
    }
    /**
     * Unread the bytes.  The next call to read will
     * return b[start].
     */
    public void unread(byte[] b, int start, int len)
        throws IOException {
        ensureOpen();
        m_stack.push(b, start, len);
    }


    @Override
    public int read()
        throws IOException {
        ensureOpen();
        return m_stack.isEmpty() ? super.read() : m_stack.pop();
    }

    @Override
    public int read(byte[] b, int off, int len)
        throws IOException {
        ensureOpen();

        if(!m_stack.isEmpty()) {
            int read = m_stack.pop(b, off, len);
            off+=read;
            len-=read;
            if(len > 0) {
                //Handle boundary case where the real stream
                //is empty, yet there are still buffered bytes.
                int superRead = super.read(b, off, len);
                if(superRead > 0) {
                    read+=superRead;
                }
            }
            return read;
        }
        return super.read(b, off, len);
    }

    @Override
    public void close()
        throws IOException {
        m_closed = true;
        m_stack.close();
        super.close();
    }

    @Override
    public int available()
        throws IOException {
        ensureOpen();
        return super.available() + m_stack.available();
    }

    /**
     * Mark is not supported, so this method does nothing
     *
     * @exception IOException from the backing stream
     */
    @Override
    public void mark(int readlimit) {
        //Do nothing
    }



    /**
     * Since marks are not supported, this always throws
     * an exception
     *
     * @exception IOException (always)
     */
    @Override
    public void reset()
        throws IOException {
        throw new IOException("mark not supported");
    }


    /**
     * Always returns false
     *
     * @exception IOException from the backing stream
     */
    @Override
    public boolean markSupported() {
        return false;
    }

    private void ensureOpen()
        throws IOException {
        if(m_closed) {
            throw new IOException("Stream already closed");
        }
    }

    //------------- Inner Class ---------------

    /**
     * The class which performs the real work, by maintainng
     * a Stack.  All puts are in the original order from
     * the caller, and reversed by this class.
     */
    private class ByteStack {
        private byte[] m_buf;
        private int m_expandBy;
        private int m_head;

        ByteStack(int initSz, int growBy) {
            m_buf = new byte[initSz];
            m_expandBy = growBy;
            m_head = 0;
        }

        int available() {
            return m_head;
        }
        boolean isEmpty() {
            return m_head <= 0;
        }

        int pop() {
            if(isEmpty()) {
                //TODO bscott An assert instead?
                throw new IllegalStateException("Stack is empty");
            }
            return m_buf[--m_head] & 0xff;
        }
        void push(int b) {
            ensure(1);
            m_buf[m_head++] = (byte) b;
        }
        void push(final byte[] bytes, final int start, final int len) {
            ensure(len);
            for(int i = start+len-1; i>=start; i--) {
                m_buf[m_head++] = bytes[i];
            }
        }
        int pop(final byte[] bytes, final int start, int len) {
            int avail = available();
            if(avail == 0) {
                return 0;
            }
            len = len>avail ? avail : len;

            for(int i = 0; i<len; i++) {
                bytes[start+i] = m_buf[--m_head];
            }
            return len;
        }

        void close() {
            m_buf = null;
        }

        private void ensure(int addLen) {
            int curLen = m_buf.length;
            int remainLen = curLen - m_head;
            if(remainLen < addLen) {
                // new buffer needs to grow larger than org buffer by expandLen
                int expandLen = (remainLen + m_expandBy) < addLen ? addLen : m_expandBy;
                byte[] newArray = new byte[curLen + expandLen];
                System.arraycopy(m_buf, 0, newArray, 0, m_head);
                m_buf = newArray;
            }
        }
    }//ENDOF ByteStack

    /*
      public static void main(String[] args)
      throws Exception {

      byte[] bytes = "abcdefg".getBytes();

      ByteArrayInputStream in  = new ByteArrayInputStream(bytes);

      DynPushbackInputStream dynIn = new DynPushbackInputStream(in);

      byte b1 = (byte) dynIn.read();
      byte b2 = (byte) dynIn.read();
      byte b3 = (byte) dynIn.read();

      System.out.println("a?:" + (char) b1);
      System.out.println("b?:" + (char) b2);
      System.out.println("c?:" + (char) b3);
      dynIn.unread(b3);
      b3 = (byte) dynIn.read();
      System.out.println("c?:" + (char) b3);

      byte[] readBytes = new byte[3];
      dynIn.read(readBytes);
      System.out.println("def?: " + new String(readBytes));

      byte[] cdefg = "cdef".getBytes();
      dynIn.unread(cdefg);

      dynIn.read(readBytes);
      System.out.println("cde?: " + new String(readBytes));

      int readNum = dynIn.read(readBytes);
      System.out.println("readNum == 2?: " + readNum);
      System.out.println("fg?: " + new String(readBytes, 0, readNum));

      readNum = dynIn.read(readBytes);
      System.out.println("readNum == -1?: " + readNum);


      }
    */
}//ENDOF DynPushbackInputStream
