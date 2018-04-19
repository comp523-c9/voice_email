//package com.cloudnine.emailclerk;
//
//import java.util.Iterator;
//import java.util.NoSuchElementException;
//
//public class EmailIterable<T extends Email> implements Iterable<T>
//{
//    private static final int MARGIN = 10;
//    private static final int BUFFER = 50;
//
//    public Iterator<T> iterator()
//    {
//        return new EmailIterator<>(null, null);
//    }
//
//    class EmailIterator<T> implements Iterator<T>
//    {
//        private T[] storedEmails;
//        private EmailController controller;
//        private int pointer;
//
//        public EmailIterator(EmailController controller, T[] emails)
//        {
//            this.storedEmails = emails;
//            this.controller = controller;
//            this.pointer = 0;
//        }
//
//        public boolean hasNext()
//        {
//            return pointer < BUFFER - 1;
//        }
//
//        public T next()
//        {
//            if(!hasNext())
//            {
//                throw new NoSuchElementException();
//            }
//            else
//            {
//                if(BUFFER - pointer < MARGIN)
//                {
//                    controller.getNewEmails(10);
//                }
//
//                pointer++;
//                return storedEmails[pointer];
//            }
//        }
//    }
//}
