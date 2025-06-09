import com.oocourse.library3.LibraryBookIsbn;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class User {
    public static final int INITIAL_CREDIT = 100;
    public static final int MAX_CREDIT = 180;
    public static final int MIN_CREDIT = 0;
    public static final int MIN_CREDIT_FOR_READING_BC = 1;
    public static final int MIN_CREDIT_FOR_READING_A = 40;
    public static final int MIN_CREDIT_FOR_BORROWING = 60;
    public static final int MIN_CREDIT_FOR_ORDERING = 100;
    public static final int CREDIT_ADD_FOR_BORROWING = 10;
    public static final int CREDIT_ADD_FOR_READING = 10;
    public static final int CREDIT_DEDUCT_FOR_BORROWING = 5;
    public static final int CREDIT_DEDUCT_FOR_ORDERING = 15;
    public static final int CREDIT_DEDUCT_FOR_READING = 10;

    private final String id;
    private int credit = INITIAL_CREDIT;
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
        credit = Math.min(credit + amount, MAX_CREDIT);
    }

    private void reduceCredit(int amount) {
        credit = Math.max(credit - amount, MIN_CREDIT);
    }

    public boolean canBorrow(LibraryBookIsbn bookIsbn) {
        switch (bookIsbn.getType()) {
            case B:
                return borrowedTypeB.isEmpty() && credit >= MIN_CREDIT_FOR_BORROWING;
            case C:
                return !borrowedTypeCs.containsKey(bookIsbn) && credit >= MIN_CREDIT_FOR_BORROWING;
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
                reduceCredit(Math.min(daysOverdue, passedDays) * CREDIT_DEDUCT_FOR_BORROWING);
            }
        }
        for (LibraryBookIsbn bookIsbn : borrowedTypeCs.keySet()) {
            LocalDate borrowedDate = borrowedTypeCs.get(bookIsbn);
            int daysOverdue = (int) ChronoUnit.DAYS.between(borrowedDate, date) - 60;
            if (daysOverdue > 0) {
                reduceCredit(Math.min(daysOverdue, passedDays) * CREDIT_DEDUCT_FOR_BORROWING);
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
            addCredit(CREDIT_ADD_FOR_BORROWING);
        }
        return !isOverdue;
    }

    public boolean canOrder(LibraryBookIsbn bookIsbn) {
        return ordered.isEmpty() && credit >= MIN_CREDIT_FOR_ORDERING && canBorrow(bookIsbn);
    }

    public void orderBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        ordered.put(bookIsbn, date);
    }

    public void notPick() {
        ordered.clear();
        reduceCredit(CREDIT_DEDUCT_FOR_ORDERING);
    }

    public void pickBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        ordered.clear();
        borrowBook(date, bookIsbn);
    }

    public boolean canRead(LibraryBookIsbn bookIsbn) {
        switch (bookIsbn.getType()) {
            case B:
            case C:
                return read.isEmpty() && credit >= MIN_CREDIT_FOR_READING_BC;
            case A:
                return read.isEmpty() && credit >= MIN_CREDIT_FOR_READING_A;
            default:
                return false;
        }
    }

    public void readBook(LocalDate date, LibraryBookIsbn bookIsbn) {
        read.put(bookIsbn, date);
    }

    public void notRestore() {
        read.clear();
        reduceCredit(CREDIT_DEDUCT_FOR_READING);
    }

    public void restoreBook() {
        read.clear();
        addCredit(CREDIT_ADD_FOR_READING);
    }
}
