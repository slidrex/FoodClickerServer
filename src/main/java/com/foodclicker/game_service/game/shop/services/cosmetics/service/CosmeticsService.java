package com.foodclicker.game_service.game.shop.services.cosmetics.service;

import com.foodclicker.game_service.food_id.identity_service.exception.UnknownUserException;
import com.foodclicker.game_service.food_id.identity_service.util.IdentityUtil;
import com.foodclicker.game_service.game.shop.services.cosmetics.document.CosmeticsGroupElement;
import com.foodclicker.game_service.game.shop.services.cosmetics.dto.BuyCosmeticsRequest;
import com.foodclicker.game_service.game.shop.services.cosmetics.dto.EquipCosmeticsRequest;
import com.foodclicker.game_service.game.shop.services.cosmetics.entity.PlayerCosmeticsEntity;
import com.foodclicker.game_service.game.shop.services.cosmetics.entity.PlayerEquippedCosmeticsEntity;
import com.foodclicker.game_service.game.shop.services.cosmetics.exception.*;
import com.foodclicker.game_service.game.shop.services.cosmetics.model.CosmeticsGroupedItemModel;
import com.foodclicker.game_service.game.shop.services.cosmetics.model.PlayerCosmeticsGroupModel;
import com.foodclicker.game_service.game.shop.services.cosmetics.model.PlayerCosmeticsModel;
import com.foodclicker.game_service.game.shop.services.cosmetics.repository.PlayerCosmeticsRepository;
import com.foodclicker.game_service.game.shop.services.cosmetics.repository.PlayerEquippedCosmeticsRepository;
import com.foodclicker.game_service.game.shop.services.cosmetics.util.CosmeticsDocumentUtil;
import com.foodclicker.game_service.game.shop.transactions.exception.NotEnoughMoneyException;
import com.foodclicker.game_service.game.shop.transactions.util.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CosmeticsService {
    @Autowired
    private CosmeticsDocumentUtil cosmeticsDocumentUtil;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private IdentityUtil identityUtil;
    @Autowired
    private PlayerCosmeticsRepository playerCosmeticsRepository;
    @Autowired
    private PlayerEquippedCosmeticsRepository equippedCosmeticsRepository;
    public PlayerCosmeticsModel buyCosmetics(BuyCosmeticsRequest request) throws UnknownUserException, ItemAlreadyOwnedException, UnknownCosmeticsTemplateException, NotEnoughMoneyException {
        var user = identityUtil.getIncomingUser();
        var playerCosmetics = user.getPlayerCosmetics();
        if(playerCosmetics.stream().anyMatch(x -> x.getItemId() == request.getItemId() && x.getGroupId() == request.getGroupId())) throw new ItemAlreadyOwnedException();
        var documentElement = cosmeticsDocumentUtil.getCosmeticsElement(request.getGroupId(), request.getItemId());
        
        transactionService.trySpendMoney(user, documentElement.getPrice());
        
        var cosmetics = new PlayerCosmeticsEntity(user, request.getGroupId(), request.getItemId());
        playerCosmeticsRepository.save(cosmetics);
        
        return new PlayerCosmeticsModel(request.getGroupId(), request.getItemId());
    }
    public List<PlayerCosmeticsModel> getPlayerGroupCosmetics(int groupId) throws UnknownUserException {
        var user = identityUtil.getIncomingUser();
        var playerCosmetics = user.getPlayerCosmetics();
        var cosmeticsModels = new ArrayList<PlayerCosmeticsModel>();
        for (var cosmetic:
             playerCosmetics) {
            if(cosmetic.getGroupId() == groupId)
            cosmeticsModels.add(new PlayerCosmeticsModel(cosmetic));
        }
        return cosmeticsModels;
    }
    public PlayerCosmeticsModel equipCosmetics(EquipCosmeticsRequest request) throws UnknownUserException, ItemAlreadyEquippedException, ItemNotOwnedException {
        var user = identityUtil.getIncomingUser();
        var equippedItems = user.getEquippedCosmetics();
        var equippedItemsStream = equippedItems.stream();
        if(equippedItemsStream.anyMatch(x -> x.getCosmetics().getGroupId() == request.getGroupId() && x.getCosmetics().getItemId() == request.getItemId())) throw new ItemAlreadyEquippedException();
        
        var playerCosmetics = user.getPlayerCosmetics().stream().filter(x -> x.getGroupId() == request.getGroupId() && x.getItemId() == request.getItemId()).findFirst().orElse(null);
        if(playerCosmetics == null && request.getItemId() != 0) throw new ItemNotOwnedException();
        else if(playerCosmetics == null)
        {
            playerCosmetics = new PlayerCosmeticsEntity(user, request.getGroupId(), request.getItemId());
            playerCosmeticsRepository.save(playerCosmetics);
        }
        
        boolean databaseSlotCreated = equippedItems.stream().anyMatch(x -> x.getCosmetics().getGroupId() == request.getGroupId());

        
        PlayerEquippedCosmeticsEntity equippedCosmetics;
        if(databaseSlotCreated) 
        {
            equippedCosmetics = equippedItems.stream().filter(x -> x.getCosmetics().getGroupId() == request.getGroupId()).findFirst().get();
            equippedCosmetics.setCosmetics(playerCosmetics);
        }
        else equippedCosmetics = new PlayerEquippedCosmeticsEntity(playerCosmetics);
        equippedCosmeticsRepository.save(equippedCosmetics);
        
        return new PlayerCosmeticsModel(playerCosmetics);
    }
    public List<List<CosmeticsGroupElement>> getAllCosmetics() throws UnknownCosmeticsTemplateException {
        return cosmeticsDocumentUtil.getAllItems();
    }
    public List<PlayerCosmeticsGroupModel> getAllPlayerCosmetics() throws UnknownUserException, DuplicateGroupEquipmentViolation {
        var user = identityUtil.getIncomingUser();
        var playerCosmetics = user.getPlayerCosmetics();
        var responseItems = new ArrayList<PlayerCosmeticsGroupModel>(CosmeticsDocumentUtil.GROUP_COUNT);
        for(int i = 0; i < CosmeticsDocumentUtil.GROUP_COUNT; i++) responseItems.add(null);
        
        for (PlayerCosmeticsEntity playerCosmetic : playerCosmetics) {
            int iterGroupId = playerCosmetic.getGroupId();
            
            if(iterGroupId >= CosmeticsDocumentUtil.GROUP_COUNT) continue;
            var iterGroup = responseItems.get(iterGroupId);
            if (iterGroup == null) iterGroup = new PlayerCosmeticsGroupModel(0, new ArrayList<>());
            if(playerCosmetic.getEquippedCosmetics() != null) 
            {
                int equipItemId = playerCosmetic.getItemId();
                if(iterGroup.getEquippedItemId() != 0 && equipItemId != 0) throw new DuplicateGroupEquipmentViolation(iterGroupId, iterGroup.getEquippedItemId(), equipItemId);
                iterGroup.setEquippedItemId(equipItemId);
            }
            
            iterGroup.getItems().add(new CosmeticsGroupedItemModel(playerCosmetic.getItemId()));
            responseItems.set(iterGroupId, iterGroup);
        }
        return responseItems;
    }
}
