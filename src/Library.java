import com.oocourse.library1.LibraryBookId;
import com.oocourse.library1.LibraryBookIsbn;
import com.oocourse.library1.LibraryBookIsbn.Type;
import com.oocourse.library1.LibraryMoveInfo;
import com.oocourse.library1.LibraryTrace;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.oocourse.library1.LibraryBookState.APPOINTMENT_OFFICE;
import static com.oocourse.library1.LibraryBookState.BOOKSHELF;
import static com.oocourse.library1.LibraryBookState.BORROW_RETURN_OFFICE;
import static com.oocourse.library1.LibraryBookState.USER;

public class Library {
    private final Map<LibraryBookId, Book> inventory = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> bookshelf = new HashMap<>();
    private final Map<String, Pair<Integer, LibraryBookId>> appointmentOffice = new HashMap<>();
    private final Map<LibraryBookIsbn, Set<LibraryBookId>> borrowReturnOffice = new HashMap<>();
    private final Map<String, User> users = new HashMap<>();

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
                this.inventory.put(bookId, new Book(bookId));
            }
            bookshelf.put(bookIsbn, bookIds);
            borrowReturnOffice.put(bookIsbn, new HashSet<>());
        }
    }

    public List<LibraryMoveInfo> open(LocalDate date) {
        List<LibraryMoveInfo> infos = new ArrayList<>();
        for (Set<LibraryBookId> bookIds : borrowReturnOffice.values()) {
            Iterator<LibraryBookId> bookIditerator = bookIds.iterator();
            while (bookIditerator.hasNext()) {
                LibraryBookId bookId = bookIditerator.next();
                bookIditerator.remove();
                inventory.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                infos.add(new LibraryMoveInfo(bookId, BORROW_RETURN_OFFICE, BOOKSHELF));
            }
        }
        Iterator<String> userIdIterator = appointmentOffice.keySet().iterator();
        while (userIdIterator.hasNext()) {
            String userId = userIdIterator.next();
            Pair<Integer, LibraryBookId> pair = appointmentOffice.get(userId);
            if (pair.getFirst() == 0) {
                LibraryBookId bookId = pair.getSecond();
                userIdIterator.remove();
                appointments.remove(userId);
                inventory.get(bookId).move(date, BOOKSHELF);
                bookshelf.get(bookId.getBookIsbn()).add(bookId);
                infos.add(new LibraryMoveInfo(bookId, APPOINTMENT_OFFICE, BOOKSHELF));
            } else {
                appointmentOffice.put(userId, new Pair<>(pair.getFirst() - 1, pair.getSecond()));
            }
        }
        return infos;
    }

    public List<LibraryMoveInfo> close(LocalDate date) {
        List<LibraryMoveInfo> infos = new ArrayList<>();
        for (String userId : appointments.keySet()) {
            LibraryBookIsbn bookIsbn = appointments.get(userId);
            if (!borrowReturnOffice.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = borrowReturnOffice.get(bookIsbn).iterator().next();
                borrowReturnOffice.get(bookIsbn).remove(bookId);
                inventory.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(6, bookId));
                infos.add(new LibraryMoveInfo(bookId, BORROW_RETURN_OFFICE, APPOINTMENT_OFFICE,
                    userId));
            } else if (!bookshelf.get(bookIsbn).isEmpty()) {
                LibraryBookId bookId = bookshelf.get(bookIsbn).iterator().next();
                bookshelf.get(bookIsbn).remove(bookId);
                inventory.get(bookId).move(date, APPOINTMENT_OFFICE);
                appointmentOffice.put(userId, new Pair<>(6, bookId));
                infos.add(new LibraryMoveInfo(bookId, BOOKSHELF, APPOINTMENT_OFFICE, userId));
            }
        }
        return infos;
    }

    public List<LibraryTrace> queryTrace(LibraryBookId bookId) {
        return Collections.unmodifiableList(inventory.get(bookId).getTrace());
    }

    public LibraryBookId borrowBook(LocalDate date, LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (bookshelf.get(bookIsbn).isEmpty()) {
            return null;
        } else {
            LibraryBookId bookId = bookshelf.get(bookIsbn).iterator().next();
            if (users.get(userId).canBorrow(bookIsbn)) {
                bookshelf.get(bookIsbn).remove(bookId);
                inventory.get(bookId).move(date, USER);
                users.get(userId).borrowBook(bookIsbn);
                return bookId;
            } else {
                return null;
            }
        }
    }

    public boolean orderBook(LibraryBookIsbn bookIsbn, String userId) {
        users.putIfAbsent(userId, new User(userId));
        if (appointments.containsKey(userId)) {
            return false;
        } else if (users.get(userId).canBorrow(bookIsbn)) {
            appointments.put(userId, bookIsbn);
            return true;
        } else {
            return false;
        }
    }

    public void returnBook(LocalDate date, LibraryBookId bookId, String userId) {
        users.get(userId).returnBook(bookId.getBookIsbn());
        inventory.get(bookId).move(date, BORROW_RETURN_OFFICE);
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
            appointments.remove(userId);
            inventory.get(bookId).move(date, USER);
            users.get(userId).borrowBook(bookIsbn);
            return bookId;
        }
    }
}
