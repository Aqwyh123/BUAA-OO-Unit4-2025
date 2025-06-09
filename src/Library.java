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
    private LocalDate lastOpenDate = null;
    private final Map<LibraryBookId, Book> books = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> bookshelf = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> hotBookshelf = new HashMap<>();
    private final Map<String, LibraryBookId> readingRoom = new HashMap<>();
    private final Map<String, Pair<LibraryBookId, LocalDate>> appointmentOffice = new HashMap<>();
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
        Iterator<String> orderedUserIds = appointmentOffice.keySet().iterator();
        while (orderedUserIds.hasNext()) {
            String userId = orderedUserIds.next();
            Pair<LibraryBookId, LocalDate> pair = appointmentOffice.get(userId);
            if (ChronoUnit.DAYS.between(pair.getSecond(), date) >= 5) {
                LibraryBookId bookId = pair.getFirst();
                orderedUserIds.remove();
                books.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                users.get(userId).notPick();
                infos.add(new LibraryMoveInfo(bookId, APPOINTMENT_OFFICE, BOOKSHELF));
            }
        }
        Iterator<String> orderingUserIds = appointments.keySet().iterator();
        while (orderingUserIds.hasNext()) {
            String userId = orderingUserIds.next();
            LibraryBookIsbn bookIsbn = appointments.get(userId);
            if (!bookshelf.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = bookshelf.get(bookIsbn).iterator().next();
                bookshelf.get(bookIsbn).remove(bookId);
                books.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(bookId, date));
                orderingUserIds.remove();
                infos.add(new LibraryMoveInfo(bookId, BOOKSHELF, APPOINTMENT_OFFICE, userId));
            }
        }
        for (LibraryBookIsbn bookIsbn : hotBooks) {
            for (LibraryBookId bookId : bookshelf.get(bookIsbn)) {
                hotBookshelf.get(bookId.getBookIsbn()).add(bookId);
                books.get(bookId).move(date, HOT_BOOKSHELF);
                infos.add(new LibraryMoveInfo(bookId, BOOKSHELF, HOT_BOOKSHELF));
            }
            bookshelf.get(bookIsbn).clear();
        }
        hotBooks.clear();
        lastOpenDate = lastOpenDate == null ? date : lastOpenDate;
        int passedDays = (int) ChronoUnit.DAYS.between(lastOpenDate, date) - 1;
        if (passedDays > 0) {
            users.values().forEach(user -> user.checkReturn(date, passedDays));
        }
        lastOpenDate = date;
        return infos;
    }

    public List<LibraryMoveInfo> close(LocalDate date) {
        List<LibraryMoveInfo> infos = new ArrayList<>();
        for (Set<LibraryBookId> bookIds : borrowReturnOffice.values()) {
            for (LibraryBookId bookId : bookIds) {
                books.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                infos.add(new LibraryMoveInfo(bookId, BORROW_RETURN_OFFICE, BOOKSHELF));
            }
            bookIds.clear();
        }
        for (String userId : readingRoom.keySet()) {
            LibraryBookId bookId = readingRoom.get(userId);
            books.get(bookId).move(date, BOOKSHELF);
            bookshelf.get(bookId.getBookIsbn()).add(bookId);
            users.get(userId).notRestore();
            infos.add(new LibraryMoveInfo(bookId, READING_ROOM, BOOKSHELF));
        }
        readingRoom.clear();
        for (Set<LibraryBookId> bookIds : hotBookshelf.values()) {
            for (LibraryBookId bookId : bookIds) {
                books.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                infos.add(new LibraryMoveInfo(bookId, HOT_BOOKSHELF, BOOKSHELF));
            }
            bookIds.clear();
        }
        users.values().forEach(user -> user.checkReturn(date, 1));
        return infos;
    }

    public List<LibraryTrace> queryTrace(LibraryBookId bookId) {
        return Collections.unmodifiableList(books.get(bookId).getTrace());
    }

    public int queryCredit(String userId) {
        users.putIfAbsent(userId, new User(userId));
        return users.get(userId).getCredit();
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
            users.get(userId).borrowBook(date, bookIsbn);
            hotBooks.add(bookId.getBookIsbn());
            return bookId;
        }
    }

    public boolean orderBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (!users.get(userId).canOrder(bookIsbn)) {
            return false;
        } else {
            users.get(userId).orderBook(date, bookIsbn);
            appointments.put(userId, bookIsbn);
            return true;
        }
    }

    public boolean returnBook(LocalDate date, LibraryBookId bookId, String userId) {
        boolean isOverdue = users.get(userId).returnBook(date, bookId.getBookIsbn());
        books.get(bookId).move(date, BORROW_RETURN_OFFICE);
        borrowReturnOffice.get(bookId.getBookIsbn()).add(bookId);
        return !isOverdue;
    }

    public LibraryBookId pickBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        if (!appointmentOffice.containsKey(userId)) {
            return null;
        } else if (!appointmentOffice.get(userId).getFirst().getBookIsbn().equals(bookIsbn)) {
            return null;
        } else if (!users.get(userId).canBorrow(bookIsbn)) {
            return null;
        } else {
            LibraryBookId bookId = appointmentOffice.get(userId).getFirst();
            appointmentOffice.remove(userId);
            books.get(bookId).move(date, USER);
            users.get(userId).pickBook(date, bookIsbn);
            return bookId;
        }
    }

    public LibraryBookId readBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (bookshelf.get(bookIsbn).isEmpty() && hotBookshelf.get(bookIsbn).isEmpty()) {
            return null;
        } else if (!users.get(userId).canRead(bookIsbn)) {
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
            users.get(userId).readBook(date, bookIsbn);
            readingRoom.put(userId, bookId);
            hotBooks.add(bookId.getBookIsbn());
            return bookId;
        }
    }

    public void restoreBook(LocalDate date, LibraryBookId bookId, String userId) {
        readingRoom.remove(userId);
        users.get(userId).restoreBook();
        books.get(bookId).move(date, BORROW_RETURN_OFFICE);
        borrowReturnOffice.get(bookId.getBookIsbn()).add(bookId);
    }
}
