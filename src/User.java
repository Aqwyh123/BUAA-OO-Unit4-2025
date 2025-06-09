import com.oocourse.library3.LibraryBookIsbn;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class User {
    private final String id;
    private int credit = 100;
    private final Map<LibraryBookIsbn, LocalDate> borrowedTypeB = new HashMap<>();
    private final Map<LibraryBookIsbn, LocalDate> borrowedTypeCs = new HashMap<>();
    private final Map<LibraryBookIsbn, LocalDate> ordered = new HashMap<>();
    private final Map<LibraryBookIsbn, LocalDate> read = new HashMap<>();

    public User(String id) {
        this.id = id;
    }

    public int getCredit() {
        return credit;
    }

    private void addCredit(int amount) {
        credit = Math.min(credit + amount, 180);
    }

    private void reduceCredit(int amount) {
        credit = Math.max(credit - amount, 0);
    }

    public boolean canBorrow(LibraryBookIsbn bookIsbn) {
        switch (bookIsbn.getType()) {
            case B:
                return borrowedTypeB.isEmpty() && credit >= 60;
            case C:
                return !borrowedTypeCs.containsKey(bookIsbn) && credit >= 60;
            case A:
            default:
                return false;
        }
    }

    public void borrowBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        switch (bookIsbn.getType()) {
            case B:
                borrowedTypeB.put(bookIsbn, date);
                break;
            case C:
                borrowedTypeCs.put(bookIsbn, date);
                break;
            case A:
            default:
        }
    }

    public void checkReturn(LocalDate date, int passedDays) {
        for (LibraryBookIsbn bookIsbn : borrowedTypeB.keySet()) {
            LocalDate borrowedDate = borrowedTypeB.get(bookIsbn);
            int daysOverdue = (int) ChronoUnit.DAYS.between(borrowedDate, date) - 30;
            if (daysOverdue > 0) {
                reduceCredit(Math.min(daysOverdue, passedDays) * 5);
            }
        }
        for (LibraryBookIsbn bookIsbn : borrowedTypeCs.keySet()) {
            LocalDate borrowedDate = borrowedTypeCs.get(bookIsbn);
            int daysOverdue = (int) ChronoUnit.DAYS.between(borrowedDate, date) - 60;
            if (daysOverdue > 0) {
                reduceCredit(Math.min(daysOverdue, passedDays) * 5);
            }
        }
    }

    public boolean returnBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        boolean isOverdue;
        switch (bookIsbn.getType()) {
            case B:
                isOverdue = ChronoUnit.DAYS.between(borrowedTypeB.get(bookIsbn), date) > 30;
                borrowedTypeB.clear();
                break;
            case C:
                isOverdue = ChronoUnit.DAYS.between(borrowedTypeCs.get(bookIsbn), date) > 60;
                borrowedTypeCs.remove(bookIsbn);
                break;
            case A:
            default:
                throw new IllegalArgumentException("Cannot borrow/return book of type A!");
        }
        if (!isOverdue) {
            addCredit(10);
        }
        return !isOverdue;
    }

    public boolean canOrder(LibraryBookIsbn bookIsbn) {
        return ordered.isEmpty() && credit >= 100 && canBorrow(bookIsbn);
    }

    public void orderBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        ordered.put(bookIsbn, date);
    }

    public void notPick() {
        ordered.clear();
        reduceCredit(15);
    }

    public void pickBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        ordered.clear();
        borrowBook(date, bookIsbn);
    }

    public boolean canRead(LibraryBookIsbn bookIsbn) {
        switch (bookIsbn.getType()) {
            case B:
            case C:
                return read.isEmpty() && credit >= 1;
            case A:
                return read.isEmpty() && credit >= 40;
            default:
                return false;
        }
    }

    public void readBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        read.put(bookIsbn, date);
    }

    public void notRestore() {
        read.clear();
        reduceCredit(10);
    }

    public void restoreBook() {
        read.clear();
        addCredit(10);
    }
}
