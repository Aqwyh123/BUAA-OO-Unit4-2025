import com.oocourse.library3.LibraryBookId;
import com.oocourse.library3.LibraryBookIsbn;
import com.oocourse.library3.LibraryBookIsbn.Type;
import com.oocourse.library3.LibraryMoveInfo;
import com.oocourse.library3.LibraryTrace;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.oocourse.library3.LibraryBookState.APPOINTMENT_OFFICE;
import static com.oocourse.library3.LibraryBookState.BOOKSHELF;
import static com.oocourse.library3.LibraryBookState.BORROW_RETURN_OFFICE;
import static com.oocourse.library3.LibraryBookState.HOT_BOOKSHELF;
import static com.oocourse.library3.LibraryBookState.READING_ROOM;
import static com.oocourse.library3.LibraryBookState.USER;

public class Library {
    private final Map<LibraryBookId, Book> books = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> bookshelf = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> hotBookshelf = new HashMap<>();
    private final Map<String, LibraryBookId> readingRoom = new HashMap<>();
    private final Map<String, Pair<LocalDate, LibraryBookId>> appointmentOffice = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> borrowReturnOffice = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();

    private final Set<LibraryBookIsbn> hotBooks = new HashSet<>();
    private final Map<String, LibraryBookIsbn> appointments = new HashMap<>();

    public Library(Map<LibraryBookIsbn, Integer> inventory) {
        for (LibraryBookIsbn bookIsbn : inventory.keySet()) {
            Set<LibraryBookId> bookIds = new HashSet<>();
            for (int i = 1; i <= inventory.get(bookIsbn); i++) {
                Type bookType = bookIsbn.getType();
                String bookUid = bookIsbn.getUid();
                String copyId = i < 10 ? "0" + i : String.valueOf(i);
                LibraryBookId bookId = new LibraryBookId(bookType, bookUid, copyId);
                bookIds.add(bookId);
                books.put(bookId, new Book(bookId));
            }
            bookshelf.put(bookIsbn, bookIds);
            hotBookshelf.put(bookIsbn, new HashSet<>());
            borrowReturnOffice.put(bookIsbn, new HashSet<>());
        }
    }

    public List<LibraryMoveInfo> open(LocalDate date) {
        List<LibraryMoveInfo> infos = new ArrayList<>();
        for (Set<LibraryBookId> bookIds : borrowReturnOffice.values()) {
            for (LibraryBookId bookId : bookIds) {
                books.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                infos.add(new LibraryMoveInfo(bookId, BORROW_RETURN_OFFICE, BOOKSHELF));
            }
            bookIds.clear();
        }
        for (LibraryBookId bookId : readingRoom.values()) {
            books.get(bookId).move(date, BOOKSHELF);
            bookshelf.get(bookId.getBookIsbn()).add(bookId);
            infos.add(new LibraryMoveInfo(bookId, READING_ROOM, BOOKSHELF));
        }
        readingRoom.clear();
        Iterator<String> userIdIterator = appointmentOffice.keySet().iterator();
        while (userIdIterator.hasNext()) {
            String userId = userIdIterator.next();
            Pair<LocalDate, LibraryBookId> pair = appointmentOffice.get(userId);
            if (ChronoUnit.DAYS.between(pair.getFirst(), date) >= 5) {
                LibraryBookId bookId = pair.getSecond();
                userIdIterator.remove();
                books.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                users.get(userId).cancelOrder();
                infos.add(new LibraryMoveInfo(bookId, APPOINTMENT_OFFICE, BOOKSHELF));
            }
        }
        for (Set<LibraryBookId> bookIds : hotBookshelf.values()) {
            Iterator<LibraryBookId> bookIdIterator = bookIds.iterator();
            while (bookIdIterator.hasNext()) {
                LibraryBookId bookId = bookIdIterator.next();
                if (!hotBooks.contains(bookId.getBookIsbn())) {
                    bookIdIterator.remove();
                    books.get(bookId).move(date, BOOKSHELF);
                    bookshelf.get(bookId.getBookIsbn()).add(bookId);
                    infos.add(new LibraryMoveInfo(bookId, HOT_BOOKSHELF, BOOKSHELF));
                }
            }
        }
        for (Set<LibraryBookId> bookIds : bookshelf.values()) {
            Iterator<LibraryBookId> bookIdIterator = bookIds.iterator();
            while (bookIdIterator.hasNext()) {
                LibraryBookId bookId = bookIdIterator.next();
                if (hotBooks.contains(bookId.getBookIsbn())) {
                    bookIdIterator.remove();
                    hotBookshelf.get(bookId.getBookIsbn()).add(bookId);
                    books.get(bookId).move(date, HOT_BOOKSHELF);
                    infos.add(new LibraryMoveInfo(bookId, BOOKSHELF, HOT_BOOKSHELF));
                }
            }
        }
        hotBooks.clear();
        return infos;
    }

    public List<LibraryMoveInfo> close(LocalDate date) {
        List<LibraryMoveInfo> infos = new ArrayList<>();
        Iterator<String> userIdIterator = appointments.keySet().iterator();
        while (userIdIterator.hasNext()) {
            String userId = userIdIterator.next();
            LibraryBookIsbn bookIsbn = appointments.get(userId);
            if (!borrowReturnOffice.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = borrowReturnOffice.get(bookIsbn).iterator().next();
                borrowReturnOffice.get(bookIsbn).remove(bookId);
                books.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(date.plusDays(1), bookId));
                userIdIterator.remove();
                infos.add(new LibraryMoveInfo(bookId, BORROW_RETURN_OFFICE, APPOINTMENT_OFFICE,
                    userId));
            } else if (!bookshelf.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = bookshelf.get(bookIsbn).iterator().next();
                bookshelf.get(bookIsbn).remove(bookId);
                books.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(date.plusDays(1), bookId));
                userIdIterator.remove();
                infos.add(new LibraryMoveInfo(bookId, BOOKSHELF, APPOINTMENT_OFFICE, userId));
            } else if (!hotBookshelf.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = hotBookshelf.get(bookIsbn).iterator().next();
                hotBookshelf.get(bookIsbn).remove(bookId);
                books.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(date.plusDays(1), bookId));
                userIdIterator.remove();
                infos.add(new LibraryMoveInfo(bookId, HOT_BOOKSHELF, APPOINTMENT_OFFICE, userId));
            } else {
                Iterator<LibraryBookId> bookIdIterator = readingRoom.values().iterator();
                while (bookIdIterator.hasNext()) {
                    LibraryBookId bookId = bookIdIterator.next();
                    if (bookId.getBookIsbn().equals(bookIsbn)) {
                        bookIdIterator.remove();
                        books.get(bookId).move(date, APPOINTMENT_OFFICE);
                        appointmentOffice.put(userId, new Pair<>(date.plusDays(1), bookId));
                        infos.add(new LibraryMoveInfo(bookId, READING_ROOM, APPOINTMENT_OFFICE,
                            userId));
                        userIdIterator.remove();
                        break;
                    }
                }
            }
        }
        return infos;
    }

    public List<LibraryTrace> queryTrace(LibraryBookId bookId) {
        return Collections.unmodifiableList(books.get(bookId).getTrace());
    }

    public LibraryBookId borrowBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (bookshelf.get(bookIsbn).isEmpty() && hotBookshelf.get(bookIsbn).isEmpty()) {
            return null;
        } else if (!users.get(userId).canBorrow(bookIsbn)) {
            return null;
        } else {
            LibraryBookId bookId;
            if (!hotBookshelf.get(bookIsbn).isEmpty()) {
                bookId = hotBookshelf.get(bookIsbn).iterator().next();
                hotBookshelf.get(bookIsbn).remove(bookId);
            } else {
                bookId = bookshelf.get(bookIsbn).iterator().next();
                bookshelf.get(bookIsbn).remove(bookId);
            }
            books.get(bookId).move(date, USER);
            users.get(userId).borrowBook(bookIsbn);
            hotBooks.add(bookId.getBookIsbn());
            return bookId;
        }
    }

    public boolean orderBook(LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (!users.get(userId).canOrder(bookIsbn)) {
            return false;
        } else {
            users.get(userId).beginOrder();
            appointments.put(userId, bookIsbn);
            return true;
        }
    }

    public void returnBook(LocalDate date, LibraryBookId bookId, String userId) {
        users.get(userId).returnBook(bookId.getBookIsbn());
        books.get(bookId).move(date, BORROW_RETURN_OFFICE);
        borrowReturnOffice.get(bookId.getBookIsbn()).add(bookId);
    }

    public LibraryBookId pickBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        if (!appointmentOffice.containsKey(userId)) {
            return null;
        } else if (!appointmentOffice.get(userId).getSecond().getBookIsbn().equals(bookIsbn)) {
            return null;
        } else if (!users.get(userId).canBorrow(bookIsbn)) {
            return null;
        } else {
            LibraryBookId bookId = appointmentOffice.get(userId).getSecond();
            appointmentOffice.remove(userId);
            books.get(bookId).move(date, USER);
            users.get(userId).pickBook(bookIsbn);
            return bookId;
        }
    }

    public LibraryBookId readBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        if (bookshelf.get(bookIsbn).isEmpty() && hotBookshelf.get(bookIsbn).isEmpty()) {
            return null;
        } else if (readingRoom.containsKey(userId)) {
            return null;
        } else {
            LibraryBookId bookId;
            if (!hotBookshelf.get(bookIsbn).isEmpty()) {
                bookId = hotBookshelf.get(bookIsbn).iterator().next();
                hotBookshelf.get(bookIsbn).remove(bookId);
            } else {
                bookId = bookshelf.get(bookIsbn).iterator().next();
                bookshelf.get(bookIsbn).remove(bookId);
            }
            books.get(bookId).move(date, READING_ROOM);
            readingRoom.put(userId, bookId);
            hotBooks.add(bookId.getBookIsbn());
            return bookId;
        }
    }

    public void restoreBook(LocalDate date, LibraryBookId bookId, String userId) {
        readingRoom.remove(userId);
        books.get(bookId).move(date, BORROW_RETURN_OFFICE);
        borrowReturnOffice.get(bookId.getBookIsbn()).add(bookId);
    }
}
