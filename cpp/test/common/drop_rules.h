/*
 * Copyright (c) 2012 by Matthias Noack, Zuse Institute Berlin
 *
 * Licensed under the BSD License, see LICENSE file for details.
 *
 */

#ifndef CPP_TEST_COMMON_DROP_RULES_H_
#define CPP_TEST_COMMON_DROP_RULES_H_

namespace xtreemfs {
namespace rpc {

/** This interface is used for arbitrary implementationd of drop rules
  * for RPC requests processes by TestRPCServer.
  */
class DropRule {
 public:
  /** This method is called for every received process */
  virtual bool dropRequest(boost::uint32_t proc_id) = 0;

  /** This method is used to determine whether or not a rule can be deleted */
  virtual bool isPointless() const = 0;

  /** Helper predicate function for std::algorithm */
  static bool isPointlessPred(const DropRule* rule) {
    return rule->isPointless();
  }

  virtual ~DropRule() {};
};

class SkipMDropNRule : public DropRule {
 public:
  SkipMDropNRule(size_t skip, size_t drop) : skip_count_(skip), drop_count_(drop) {}

  virtual bool dropRequest(boost::uint32_t proc_id) {
    if (skip_count_ == 0 && drop_count_ > 0) {
      --drop_count_;
      return true;
    } else  if (skip_count_ > 0) {
      --skip_count_;
    }
    return false;

    //return skip_count_-- > 0 ? ++skip_count_ : drop_count_-- > 0 ? true : ++drop_count_;
  }

  virtual bool isPointless() const {
    return (drop_count_ == 0) && (skip_count_ == 0);
  }

 private:
  size_t skip_count_;
  size_t drop_count_;
};

class ProcIDFilterRule : public DropRule {
public:
  ProcIDFilterRule(boost::uint32_t proc_id, DropRule* rule)
      : proc_id_(proc_id), rule_(rule) {}

  virtual bool dropRequest(boost::uint32_t proc_id) {
    return proc_id == proc_id_ ? rule_->dropRequest(proc_id) : false;
  }

  virtual bool isPointless() const {
    return rule_->isPointless();
  }

private:
  boost::uint32_t proc_id_;
  boost::scoped_ptr<DropRule> rule_;
};

class DropNRule : public DropRule {
public:
  DropNRule(size_t count)
      : count_(count) {}

  virtual bool dropRequest(boost::uint32_t proc_id) {
    return count_-- > 0 ? true : ++count_;
  }

  virtual bool isPointless() const {
    return count_ == 0;
  }

private:
  size_t count_;
};

class DropByProcIDRule : public DropRule {
public:
  DropByProcIDRule(boost::uint32_t proc_id)
      : proc_id_(proc_id) {}

  virtual bool dropRequest(boost::uint32_t proc_id) {
    return proc_id_ == proc_id;
  }

  virtual bool isPointless() const {
    return false;
  }

private:
  size_t proc_id_;
};


}  // namespace rpc
}  // namespace xtreemfs

#endif
