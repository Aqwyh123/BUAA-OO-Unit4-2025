import com.oocourse.library1.LibraryBookIsbn;
import com.oocourse.library1.LibraryBookIsbn.Type;

import java.util.HashSet;
import java.util.Set;

public class User {
    private final String id;
    private boolean ordering;
    private boolean borrowedTypeB;
    private final Set<LibraryBookIsbn> borrowedTypeCs;

    public User(String id) {
        this.id = id;
        this.ordering = false;
        this.borrowedTypeB = false;
        this.borrowedTypeCs = new HashSet<>();
    }

    public boolean canOrder(LibraryBookIsbn bookIsbn) {
        return !ordering && canBorrow(bookIsbn);
    }

    public void beginOrder() {
        ordering = true;
    }

    public void cancelOrder() {
        ordering = false;
    }

    public void pickBook(LibraryBookIsbn bookIsbn) {
        ordering = false;
        borrowBook(bookIsbn);
    }

    public boolean canBorrow(LibraryBookIsbn bookIsbn) {
        Type type = bookIsbn.getType();
        switch (type) {
            case B:
                return !borrowedTypeB;
            case C:
                return !borrowedTypeCs.contains(bookIsbn);
            case A:
            default:
                return false;
        }
    }

    public void borrowBook(LibraryBookIsbn bookIsbn) {
        Type type = bookIsbn.getType();
        switch (type) {
            case B:
                borrowedTypeB = true;
                break;
            case C:
                borrowedTypeCs.add(bookIsbn);
                break;
            case A:
            default:
        }
    }

    public void returnBook(LibraryBookIsbn bookIsbn) {
        Type type = bookIsbn.getType();
        switch (type) {
            case B:
                borrowedTypeB = false;
                break;
            case C:
                borrowedTypeCs.remove(bookIsbn);
                break;
            case A:
            default:
        }
    }
}
