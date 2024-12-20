package jpabook.jpashop.sample.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jpabook.jpashop.sample.Order;
import jpabook.jpashop.sample.OrderStatus;
import jpabook.jpashop.sample.QMember;
import jpabook.jpashop.sample.QOrder;
import jpabook.jpashop.sample.api.OrderApiController;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import jpabook.jpashop.sample.controller.OrderSearch;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class OrderRepository {

    private final EntityManager em;

    public void save(Order order){
        em.persist(order);
    }

    public Order findOne(Long id){
        return em.find(Order.class, id);
    }

    public List<Order> findAll() {
        return em.createQuery("select o from Order o", Order.class)
                .getResultList();
    }

    public List<Order> findAllByString(OrderSearch orderSearch) {

        String jpql = "select o from Order o join o.member m";
        boolean isFirstCondition = true;

        //주문 상태 검색
        if (orderSearch.getOrderStatus() != null) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " o.status = :status";
        }

        //회원 이름 검색
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            if (isFirstCondition) {
                jpql += " where";
                isFirstCondition = false;
            } else {
                jpql += " and";
            }
            jpql += " m.name like :name";
        }

        TypedQuery<Order> query = em.createQuery(jpql, Order.class)
                .setMaxResults(1000);

        if (orderSearch.getOrderStatus() != null) {
            query = query.setParameter("status", orderSearch.getOrderStatus());
        }
        if (StringUtils.hasText(orderSearch.getMemberName())) {
            query = query.setParameter("name", orderSearch.getMemberName());
        }

        return query.getResultList();
    }

    public List<Order> findAll(OrderSearch orderSearch){
        JPAQueryFactory query=new JPAQueryFactory(em);
        QOrder order=QOrder.order;
        QMember member=QMember.member;

        return query
                .select(order)
                .from(order)
                .where(statusEq(orderSearch.getOrderStatus()), nameLike(orderSearch.getMemberName()))
                .join(order.member, member)
                .limit(1000)
                .fetch();
    }

    private BooleanExpression statusEq(OrderStatus orderStatus){
        if(orderStatus==null){
            return null;
        }
        return QOrder.order.status.eq(orderStatus);
    }

    private BooleanExpression nameLike(String name){
        if(!StringUtils.hasText(name)){
            return null;
        }

        return QMember.member.name.like(name);
    }

    public List<Order> findAllWithMemberDelivery() {
        return em.createQuery("select o from Order o " +
                " join fetch o.member m" +
                " join fetch o.delivery d", Order.class)
                .getResultList();
    }

    public List<OrderSimpleQueryDto> findOrderDtos(){
        return em.createQuery("select new jpabook.jpashop.sample.repository.OrderSimpleQueryDto(o.id, m.name, o.orderDate, o.status, d.address) " +
                        "from Order o " +
                "join o.member m " +
                "join o.delivery d", OrderSimpleQueryDto.class)
                .getResultList();
    }

    public List<Order> findAllWithItem() {
        return em.createQuery("select distinct o from Order o" +
                " join fetch o.member m" +
                " join fetch o.delivery d" +
                " join fetch o.orderItems oi" +
                " join fetch oi.item i", Order.class)
                .getResultList();
    }

    // 컬렉션 조회: 페이징 쿼리
    public List<Order> findAllWithMemberDelivery(int offset, int limit) {
        List<Order> orders = em.createQuery("select o from Order o " +
                        " join fetch o.member m" +
                        " join fetch o.delivery d", Order.class)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();

        return orders;
    }
}
