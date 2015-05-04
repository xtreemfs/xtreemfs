/* -----------------------------------------------------------------------------
 * std_list.i
 * ----------------------------------------------------------------------------- */

%{
#include <list>
%}

namespace std {

    template<class T> class list {
      public:
        typedef size_t size_type;
        typedef T value_type;
        typedef const value_type& const_reference;
        list();
        list(size_type n);
        size_type size() const;
        %rename(isEmpty) empty;
        bool empty() const;
        void clear();
        void reverse();
        %rename(addFirst) push_front;
        void push_front(const value_type& x);
        %rename(addLast) push_back;
        void push_back(const value_type& x);
        %rename(getFirst) front;
        const_reference front();
        %rename(getLast) back;
        const_reference back();
        %rename(removeFirst) pop_front;
        void pop_front();
        %rename(removeLast) pop_back;
        void pop_back();

        %newobject iterator() const;
        %extend {
          ListIterator<T>* iterator() const {
            return new ListIterator<T>(*$self);
          }
        }
   };
}


%javamethodmodifiers ListIterator::nextImpl "private";
%inline %{
template<class T> class ListIterator {
private:
  const	std::list<T>& _list;
	typename std::list<T>::const_iterator _iter;
public:
	ListIterator(const std::list<T>& list) : _list(list),  _iter(list.begin()) {}

	bool hasNext() const	{
		return _iter != _list.end();
	}

  const T nextImpl()	{
    const T ret = *_iter;
		_iter++;
		return ret;
	}
};
%}



// Make a iterable Java implementation of a std::list.
%define LIST(Name, CppType, JavaType)

%typemap(javainterfaces) ListIterator<CppType> "java.util.Iterator<JavaType>"
%typemap(javacode) ListIterator<CppType> %{
  public void remove() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public JavaType next() throws java.util.NoSuchElementException {
    if (!hasNext()) {
      throw new java.util.NoSuchElementException();
    }

    return nextImpl();
  }
%}

%typemap(javainterfaces) std::list<CppType> "java.lang.Iterable<JavaType>"
%typemap(javacode) std::list<CppType> %{
  public java.util.List<JavaType> toList() {
    java.util.List<JavaType> list = new java.util.LinkedList<JavaType>();
    for (JavaType elem : this) {
      list.add(elem);
    }
    return list;
  }
%}

%template ( Name ) std::list<CppType>;
%template ( Name ## Iterator ) ListIterator<CppType>;

%enddef // LIST
