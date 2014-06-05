package org.aosutils.database;

public class Clause {
	public static abstract class AbstractClause {
		private Object object;
		
		public AbstractClause(Object object) {
			this.object = object;
		}
		
		public Object getObject() {
			return object;
		}
	}
	
	public static class LessThan extends AbstractClause {
		public LessThan(Object object) {
			super(object);
		}
	}
	public static class LessThanOrEqual extends AbstractClause {
		public LessThanOrEqual(Object object) {
			super(object);
		}
	}
	public static class GreaterThan extends AbstractClause {
		public GreaterThan(Object object) {
			super(object);
		}
	}
	public static class GreaterThanOrEqual extends AbstractClause {
		public GreaterThanOrEqual(Object object) {
			super(object);
		}
	}
	public static class NotEqualTo extends AbstractClause {
		public NotEqualTo(Object object) {
			super(object);
		}
	}
}
