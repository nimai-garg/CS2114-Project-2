package emergencyDispatch;

public final class ResponseUnit
{
    private final int id;
    private final UnitType type;
    private boolean available = true;
    public ResponseUnit(int id, UnitType type)
    {
        if (id < 0 || type == null)
        {
            throw new IllegalArgumentException("Unit needs a nonnegative ID and a type");
        }
        this.id = id;
        this.type = type;
    }

    public int getId()
    {
        return id;
    }

    public UnitType getType()
    {
        return type;
    }

    public boolean isAvailable()
    {
        return available;
    }

    public void setAvailable(boolean available)
    {
        this.available = available;
    }

    // Copies the unit to protect stored availability from outside changes.
    ResponseUnit copy()
    {
        ResponseUnit result = new ResponseUnit(id, type);
        result.available = available;
        return result;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other)
        {
            return true;
        }
        if (!(other instanceof ResponseUnit))
        {
            return false;
        }
        ResponseUnit otherUnit = (ResponseUnit)other;
        return id == otherUnit.id;
    }

    @Override
    public int hashCode()
    {
        return Integer.hashCode(id);
    }

    @Override
    public String toString()
    {
        return "Unit ID: " + id + ", Type: " + type + ", Available: " + available;
    }
}
