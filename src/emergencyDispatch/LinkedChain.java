package emergencyDispatch;

// Stores entries in linked nodes without application rules.
public class LinkedChain<T>
{
    private Node<T> firstNode;
    private int numberOfEntries;

    public LinkedChain()
    {
        firstNode = null;
        numberOfEntries = 0;
    }

    public boolean add(T newEntry)
    {
        requireNonNull(newEntry);
        firstNode = new Node<T>(newEntry, firstNode);
        numberOfEntries++;
        return true;
    }

    public boolean addToEnd(T newEntry)
    {
        requireNonNull(newEntry);
        Node<T> newNode = new Node<T>(newEntry);
        if (firstNode == null)
        {
            firstNode = newNode;
        }
        else
        {
            Node<T> current = firstNode;
            while (current.next != null)
            {
                current = current.next;
            }
            current.next = newNode;
        }
        numberOfEntries++;
        return true;
    }

    public T remove()
    {
        if (firstNode == null)
        {
            return null;
        }
        T result = firstNode.data;
        firstNode = firstNode.next;
        numberOfEntries--;
        return result;
    }

    public boolean remove(T anEntry)
    {
        if (anEntry == null || firstNode == null)
        {
            return false;
        }
        if (anEntry.equals(firstNode.data))
        {
            firstNode = firstNode.next;
            numberOfEntries--;
            return true;
        }
        Node<T> previous = firstNode;
        Node<T> current = firstNode.next;
        while (current != null)
        {
            if (anEntry.equals(current.data))
            {
                previous.next = current.next;
                numberOfEntries--;
                return true;
            }
            previous = current;
            current = current.next;
        }
        return false;
    }

    public void clear()
    {
        firstNode = null;
        numberOfEntries = 0;
    }

    public T get(int index)
    {
        if (index < 0 || index >= numberOfEntries)
        {
            throw new IndexOutOfBoundsException(
                "Index " + index + " out of bounds for size "
                + numberOfEntries);
        }
        Node<T> current = firstNode;
        for (int i = 0; i < index; i++)
        {
            current = current.next;
        }
        return current.data;
    }

    public boolean contains(T anEntry)
    {
        return count(anEntry) > 0;
    }

    public int count(T anEntry)
    {
        if (anEntry == null)
        {
            return 0;
        }
        int frequency = 0;
        Node<T> current = firstNode;
        while (current != null)
        {
            if (anEntry.equals(current.data))
            {
                frequency++;
            }
            current = current.next;
        }
        return frequency;
    }

    public int size()
    {
        return numberOfEntries;
    }

    public boolean isEmpty()
    {
        return numberOfEntries == 0;
    }

    public T[] toArray(T[] values)
    {
        if (values == null)
        {
            throw new IllegalArgumentException("Array must not be null");
        }
        if (values.length < numberOfEntries)
        {
            throw new IllegalArgumentException(
                "Array of length " + values.length
                + " is too small for " + numberOfEntries + " entries");
        }
        int index = 0;
        Node<T> current = firstNode;
        while (current != null)
        {
            values[index] = current.data;
            index++;
            current = current.next;
        }
        return values;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = firstNode;
        while (current != null)
        {
            sb.append(current.data);
            if (current.next != null)
            {
                sb.append(", ");
            }
            current = current.next;
        }
        sb.append("]");
        return sb.toString();
    }

    private void requireNonNull(T entry)
    {
        if (entry == null)
        {
            throw new IllegalArgumentException("Cannot add null to chain");
        }
    }

    private static class Node<E>
    {
        private E data;
        private Node<E> next;

        private Node(E dataPortion)
        {
            this(dataPortion, null);
        }

        private Node(E dataPortion, Node<E> nextNode)
        {
            data = dataPortion;
            next = nextNode;
        }
    }
}
