package com.apriori.collections;

import com.apriori.vars.Variable;

import java.util.Iterator;
import java.util.Objects;

/**
 * A fully persistent list backed by an array-mapped trie. This structure supports inexpensive
 * insertions both at the beginning and end of the list. Inexpensive means that only the path to the
 * first or last leaf trie node is copied in these cases. Insertions or removals from the middle,
 * however, are expensive and require linear runtime complexity and space overhead. Such operations
 * mean that much of the list must be copied to form the new list.
 *
 * @param <E> the type of element in the list
 * 
 * @author Joshua Humphries (jhumphries131@gmail.com)
 */
// TODO: javadoc
// TODO: tests
public class AmtPersistentList<E> extends AbstractRandomAccessImmutableList<E>
      implements PersistentList<E>, BidiIterable<E> {

   private static final AmtPersistentList<Object> EMPTY = new AmtPersistentList<>(null, 0, 0, 0, 0);
   
   @SuppressWarnings("unchecked") // safe due to immutability
   public static <E> AmtPersistentList<E> create() {
      return (AmtPersistentList<E>) EMPTY;
   }

   public static <E> AmtPersistentList<E> create(Iterable<? extends E> items) {
      return create(items.iterator());
   }
   
   public static <E> AmtPersistentList<E> create(Iterator<? extends E> iter) {
      // TODO
      return null;
   }
   
   private static class AddResult<E> {
      TrieNode<E> node;
      boolean isNew;
      int offsetForHeadExpansion;
   }
   
   private interface TrieNode<E> {
      E get(int index, int currentOffset);
      TrieNode<E> set(int index, int currentOffset, E value);
      AddResult<E> addFirst(int currentOffset, E value);
      AddResult<E> addLast(int currentOffset, E value);
   }
   
   @SuppressWarnings("unchecked")
   static <E> E[] createChildren(int length) {
      return (E[]) new Object[length];
   }
   
   private static class IntermediateTrieNode<E> implements TrieNode<E> {
      final TrieNode<E> children[];
      final int offset;
      
      IntermediateTrieNode(TrieNode<E> child, int offset) {
         TrieNode<E> ch[] = createChildren(1);
         ch[0] = child;
         this.children = ch;
         this.offset = offset;
      }

      IntermediateTrieNode(TrieNode<E> children[], int offset) {
         this.children = children;
         this.offset = offset;
      }
      
      int childIndex(int index) {
         return index - offset;
      }
      
      @Override
      public E get(int index, int currentOffset) {
         assert currentOffset >= 4;
         int childIndex = childIndex((index >>> currentOffset) & 0xff);
         return children[childIndex].get(index, currentOffset - 4);
      }

      @Override
      public TrieNode<E> set(int index, int currentOffset, E value) {
         assert currentOffset >= 4;
         int childIndex = childIndex((index >>> currentOffset) & 0xff);
         TrieNode<E> child = children[childIndex];
         TrieNode<E> newChild = child.set(index, currentOffset - 4, value);
         if (newChild == child) {
            return this;
         }
         TrieNode<E> newChildren[] = children.clone();
         newChildren[childIndex] = newChild;
         return new IntermediateTrieNode<>(newChildren, offset);
      }

      @Override
      public AddResult<E> addFirst(int currentOffset, E value) {
         assert currentOffset >= 4;
         AddResult<E> result = children[0].addFirst(currentOffset - 4, value);
         if (!result.isNew) {
            TrieNode<E> newChildren[] = children.clone();
            newChildren[0] = result.node;
            result.node = new IntermediateTrieNode<>(newChildren, offset);
            return result;
         } else {
            int len = children.length;
            if (len < 16) {
               TrieNode<E> newChildren[] = createChildren(len + 1);
               newChildren[0] = result.node;
               System.arraycopy(children, 0, newChildren, 1, len);
               result.node = new IntermediateTrieNode<>(newChildren, Math.max(0, offset - 1));
               result.isNew = false;
               return result;
            } else {
               result.node = new IntermediateTrieNode<>(result.node, 0xff);
               result.offsetForHeadExpansion = currentOffset;
               return result;
            }
         }
      }

      @Override
      public AddResult<E> addLast(int currentOffset, E value) {
         assert currentOffset >= 4;
         int len = children.length;
         AddResult<E> result = children[len - 1].addLast(currentOffset - 4, value);
         if (!result.isNew) {
            TrieNode<E> newChildren[] = children.clone();
            newChildren[len - 1] = result.node;
            result.node = new IntermediateTrieNode<>(newChildren, offset);
            return result;
         } else if (len < 16) {
            TrieNode<E> newChildren[] = createChildren(len + 1);
            System.arraycopy(children, 0, newChildren, 0, len);
            newChildren[len] = result.node;
            result.node = new IntermediateTrieNode<>(newChildren, offset);
            result.isNew = false;
            return result;
         } else {
            result.node = new IntermediateTrieNode<>(result.node, 0);
            result.offsetForHeadExpansion = currentOffset;
            return result;
         }
      }
   }
   
   private static class LeafTrieNode<E> implements TrieNode<E> {
      final E values[];

      LeafTrieNode(E value) {
         E v[] = createChildren(1);
         v[0] = value;
         this.values = v;
      }

      LeafTrieNode(E values[]) {
         this.values = values;
      }
      
      int childIndex(int index) {
         return index;
      }

      @Override
      public E get(int index, int currentOffset) {
         assert currentOffset == 0;
         return values[childIndex(index & 0xff)];
      }

      @Override
      public TrieNode<E> set(int index, int currentOffset, E value) {
         assert currentOffset == 0;
         int childIndex = childIndex(index & 0xff);
         if (Objects.equals(value, values[childIndex])) {
            return this;
         }
         E newValues[] = values.clone();
         newValues[childIndex] = value;
         return new LeafTrieNode<>(newValues);
      }

      @Override
      public AddResult<E> addFirst(int currentOffset, E value) {
         assert currentOffset == 0;
         // TODO: implement me
         return null;
         
      }

      @Override
      public AddResult<E> addLast(int currentOffset, E value) {
         assert currentOffset == 0;
         // TODO: implement me
         return null;
      }
   }
   
   private static class Trailer<E> {
      final LeafTrieNode<E> node;
      final int count;
      Trailer<E> next;
      
      Trailer(LeafTrieNode<E> node) {
         this(node, node.values.length, null);
      }

      Trailer(LeafTrieNode<E> node, int count) {
         this(node, count, null);
      }
      
      Trailer(LeafTrieNode<E> node, int count, Trailer<E> next) {
         this.node = node;
         this.count = count;
         this.next = next;
      }
   }
   
   private class Inserter {
      int numAdded;
      
      TrieNode<E> insert(TrieNode<E> node, int offset, int shift, Iterator<E> iter, int insertIdx,
            Variable<Trailer<E>> trailers) {
         if (node instanceof LeafTrieNode) {
            assert shift == 0;
         } else {
            IntermediateTrieNode<E> n = (IntermediateTrieNode<E>) node;
            int inc = 1 << shift;
            int len = n.children.length;
            if (offset == 0 && headCapacity > 0) {
               offset += (len - 16) * inc;
            }
            for (int i = 0; i < len; i++) {
               TrieNode<E> child = n.children[i];
            }
         }
         return null;
      }
      
      TrieNode<E> insert(Iterator<E> iter, int insertIdx) {
         if (root == null) {
            // TODO
            return null;
         } else {
            Variable<Trailer<E>> trailers = new Variable<>();
            TrieNode<E> newRoot = insert(root, -headCapacity, depth << 2, iter, insertIdx, trailers);
            if (iter.hasNext() || trailers.get() != null) {
               // TODO: need a deeper trie to store the rest of the elements
            }
            return newRoot;
         }
      }
   }
   
   final TrieNode<E> root;
   final int depth;
   final int size;
   final int headCapacity;
   final int headElements;

   private AmtPersistentList(TrieNode<E> root, int depth, int size, int headCapacity,
         int headElements) {
      this.root = root;
      this.depth = depth;
      this.size = size;
      this.headCapacity = headCapacity;
      this.headElements = headElements;
   }
   
   private AmtPersistentList(TrieNode<E> root, int depth, int size) {
      this(root, depth, size, 0, 0);
   }

   @Override
   public E get(int i) {
      rangeCheck(i);
      return root.get(i + headCapacity - headElements, depth << 2);
   }
   
   /*private E get(TrieNode<E> node, int index, int currentOffset) {
      if (currentOffset == 0) {
         @SuppressWarnings("unchecked")
         E ret = (E) node[index & 0xff];
         return ret;
      }
      int currIndex = (index >>> currentOffset) & 0xff;
      Object child[] = (Object[]) node[currIndex];
      return get(child, index, currentOffset - 4);
   }*/

   @Override
   public int size() {
      return size;
   }

   @Override
   public AmtPersistentList<E> subList(int from, int to) {
      rangeCheckWide(from);
      rangeCheckWide(to);
      if (from == to) {
         return create();
      }
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> add(int i, E e) {
      if (i == size) {
         return addLast(e);
      } else if (i == 0) {
         return addFirst(e);
      }
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> addAll(int i, Iterable<? extends E> items) {
      if (i == size) {
         return addAll(items);
      }
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> addFirst(E e) {
      if (size == 0) {
         TrieNode<E> newRoot = new LeafTrieNode<>(e);
         // TODO: return new AmtPersistentList<>(newRoot, 0, 1);
         return null;
      } else if (headCapacity > 0 && headElements < headCapacity) {
         // we already have padding in front, so we can store the new value there
         int newFirstIndex = headCapacity - headElements - 1;
         TrieNode<E> newRoot = root.set(newFirstIndex - 1, /* TODO: ?? */ 0, e);
         // TODO: return new AmtPersistentList<>(newRoot, depth, size + 1, newFirstIndex - 1);
         return null;
      } else {
         // have to add a pad to the front
         // TODO: Object newRoot[] = addFirst(root, depth << 2, e);
         // TODO: return new AmtPersistentList<>(newRoot, depth, size + 1, 0xff);
         return null;
      }
   }
   
   /*
   private Object[] addFirst(Object node[], int currentOffset, E val) {
      long maxSizeForDepth = 1 << (currentOffset + 4);
      if (size + 16 > maxSizeForDepth) {
         
      } else {
         
      }
   }*/

   @Override
   public AmtPersistentList<E> addLast(E e) {
      if (size == 0) {
         Object newRoot[] = new Object[] { e };
         // TODO: return new AmtPersistentList<>(newRoot, 0, 1, 0);
         return null;
      } else {
         // TODO: Object newRoot[] = addLast(root, depth << 2, e);
         // TODO: return new AmtPersistentList<>(newRoot, depth, size + 1, 0xff);
         return null;
      }
   }

   @Override
   public AmtPersistentList<E> set(int i, E e) {
      rangeCheck(i);
      // TODO
      /*
      Object newRoot[] = set(root, i + firstElementIndex, depth << 2, e);
      return newRoot != root
            ? new AmtPersistentList<>(newRoot, depth, size, firstElementIndex)
            : this;
            */
      return null;
   }
   
   private Object[] set(Object node[], int index, int currentOffset, E val) {
      if (currentOffset == 0) {
         Object o = node[index & 0xff];
         if (Objects.equals(val, o)) {
            // no change
            return node;
         }
         Object newNode[] = node.clone();
         newNode[index & 0xff] = val;
         return newNode;
      }
      int currIndex = (index >>> currentOffset) & 0xff;
      Object child[] = (Object[]) node[currIndex];
      Object newChild[] = set(child, index, currentOffset - 4, val);
      if (newChild == child) {
         return node;
      }
      Object newNode[] = node.clone();
      newNode[currIndex] = newChild;
      return newNode;
   }

   @Override
   public AmtPersistentList<E> remove(int i) {
      rangeCheck(i);
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> rest() {
      return subList(1, size);
   }

   @Override
   public AmtPersistentList<E> add(E e) {
      return addLast(e);
   }

   @Override
   public AmtPersistentList<E> remove(Object o) {
      int i = indexOf(o);
      return i >= 0 ? remove(i) : this;
   }

   @Override
   public AmtPersistentList<E> removeAll(Object o) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = this;
      int i = 0;
      for (Iterator<E> iter = iterator(); iter.hasNext();) {
         E e = iter.next();
         if (Objects.equals(e, o)) {
            ret = ret.remove(i);
         } else {
            i++;
         }
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> removeAll(Iterable<?> items) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = this;
      for (Object o : items) {
         ret = ret.removeAll(o);
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> retainAll(Iterable<?> items) {
      // TODO: bulk removal?
      AmtPersistentList<E> ret = create();
      for (E e : this) {
         if (Iterables.contains(items, e)) {
            ret = ret.add(e);
         }
      }
      return ret;
   }

   @Override
   public AmtPersistentList<E> addAll(Iterable<? extends E> items) {
      // TODO: implement me
      return null;
   }

   @Override
   public AmtPersistentList<E> clear() {
      return create();
   }
}
