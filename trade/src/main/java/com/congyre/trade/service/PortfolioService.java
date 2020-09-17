package com.congyre.trade.service;

import com.congyre.trade.entity.Portfolio;
import com.congyre.trade.entity.Stock;
import com.congyre.trade.entity.Trade;
import com.congyre.trade.entity.User;
import com.congyre.trade.entity.Trade.TradeStatus;
import com.congyre.trade.repository.PortfolioRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators.ReverseArray;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;

import org.json.JSONArray;
import org.json.JSONObject;
@Service
public class PortfolioService {
    private static Logger log = Logger.getLogger(PortfolioService.class.getName());

    @Autowired
    private PortfolioRepository repo;

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserService userService;


    public JSONObject getTickerPrices(ObjectId portId){
        HashMap<String, Stock> tickerStockMapping = getStocks(portId);
        JSONObject returnObj = new JSONObject();
        double totalValuation = 0;
        JSONArray stockList = new JSONArray();
        for (String ticker : tickerStockMapping.keySet()){
            try{
                HttpResponse<JsonNode> response = Unirest
                //.get( "https://www.alphavantage.co/query?function=TIME_SERIES_INTRADAY&symbol={tickerName}&interval=1min&outputSize=compact&apikey=NQWYX4PJ8QFOUY9I")
                .get( "https://pxqzjt6ami.execute-api.us-east-1.amazonaws.com/default/filePriceFeed?ticker={tickerName}&num_days=1")
                .routeParam("tickerName", ticker)
                .asJson();
                JSONObject completeObj = response.getBody().getObject();
                log.log(Level.WARNING, completeObj.toString());
                //String lastRefreshed = completeObj.getJSONObject("Meta Data").getString("3. Last Refreshed");
                //String lastRefreshedPrice = completeObj.getJSONObject("Time Series (1min)").getJSONObject(lastRefreshed).getString("4. close");
                JSONArray arrayPrice = completeObj.getJSONArray("price_data");
                JSONArray prices = arrayPrice.getJSONArray(0);
                Double stockPrice = prices.getDouble(1);
                int quantity = tickerStockMapping.get(ticker).getAmount();
                JSONObject tickerInfo = new JSONObject();
                tickerInfo.put("ticker", ticker);
                tickerInfo.put("price", stockPrice);
                tickerInfo.put("quantity", tickerStockMapping.get(ticker).getAmount());
                //totalValuation += quantity* Double.parseDouble(stockPrice);
                totalValuation += quantity* stockPrice;
                stockList.put(tickerInfo);
                //returnObj.put(ticker, tickerInfo);
                log.log(Level.WARNING, "Ticker: " + ticker + " price: " + stockPrice);     
                }
            catch (Exception ex){
                log.log(Level.WARNING, "Incorrect Ticker:" + ticker + ex.getMessage());

            }
        }
        returnObj.put("valuation", totalValuation);
        returnObj.put("stockPrice", stockList);
        return returnObj;
    }


    public Portfolio getportfolio(ObjectId id) {
        Optional<Portfolio> retrivePortfolio = repo.findById(id);// can be empty
        if (retrivePortfolio.isPresent()){
            System.out.println("we still get here!!!!!!!!!!!!!!!!!!!!!");
            log.log(Level.INFO, "Portfolio retrieved with id: " + id);
            return retrivePortfolio.get();
        }else{
            log.log(Level.WARNING, "This portfolio with id: " + id + "does not exist in repo");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    
    public List<Trade> getTradeHistory(ObjectId id){
        Optional<Portfolio> retrivePortfolio = repo.findById(id);// can be empty
        if (retrivePortfolio.isPresent()){
            log.log(Level.INFO, "Portfolio retrieved with id: " + id);
            log.log(Level.INFO, "Trying to get gistorical trades of the portfolio with id: " + id);
            
            Portfolio portfolio = retrivePortfolio.get();

            // get historical trades
            List<ObjectId> historicalIds = portfolio.getHistory();
            List<Trade> historicalTrades = new ArrayList<Trade>();

            for (ObjectId tradeId : historicalIds){
                Optional<Trade> getATrade = tradeService.getTradeById(tradeId); // can be empty

                // add the trade into the return list, if we can find it successfully
                if(getATrade.isPresent()){
                    Trade aTrade = getATrade.get();
                    historicalTrades.add(aTrade);
                }else{
                    log.log(Level.WARNING, "Cannot find the stock with id: " + tradeId 
                            + " in the trade history of the portfolio: " + id);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); 
                    // trade not found error happens when the addTrade function is not working properly
                }
            }
            // want the result dispalyed from the latest to the oldest
            Collections.reverse(historicalTrades);
            return historicalTrades;

        }else{
            log.log(Level.WARNING, "This portfolio with id: " + id + "does not exist in repo");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }


    public List<Trade> getPendingTrades(ObjectId id){
        Optional<Portfolio> retrivePortfolio = repo.findById(id);
        if (retrivePortfolio.isPresent()){
            log.log(Level.INFO, "Portfolio retrieved with id: " + id);
            log.log(Level.INFO, "Trying to get pending trades of the portfolio with id: " + id);
            
            Portfolio portfolio = retrivePortfolio.get();

            // get historical trades
            List<ObjectId> pendingIds = portfolio.getOutstandingList();
            List<Trade> pendingTrades = new ArrayList<Trade>();

            for (ObjectId tradeId : pendingIds){
                Optional<Trade> getATrade = tradeService.getTradeById(tradeId);// can be empty

                // add the trade into the return list, if we can find it succesfully
                if(getATrade.isPresent()){
                    Trade aTrade = getATrade.get();
                    pendingTrades.add(aTrade);
                }else{
                    log.log(Level.WARNING, "Cannot find the stock with id: " + tradeId 
                            + " in the trade history of the portfolio: " + id);
                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR); 
                    // trade not found error happens when the addTrade function is not working properly
                }
            }
            // want the result dispalyed from the latest to the oldest
            Collections.reverse(pendingTrades);
            return pendingTrades;
        
        }else{
            log.log(Level.WARNING, "This portfolio with id: " + id + "does not exist in repo");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    /*** Used for getting  live stock  data */
    public HashMap<String, Stock> getStocks(ObjectId portId){
        try{
            Portfolio portfolio = getPortfolioRepo(portId);
            HashMap<String, Stock> portfolio_stocks = portfolio.getStocks();
            return portfolio_stocks;
        }
        catch(ResponseStatusException ex){
            log.log(Level.WARNING, "This portfolio does not exist in repo");
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }

    public void addPortfolio(ObjectId userId,Portfolio port){
        //find user by id 
        User curUser = userService.getUser(userId);
        if(curUser == null) curUser = userService.updateUser(new User());
      
        //set userId of port
        port.setUserId(userId);
        //create an id for the port
        ObjectId pId = repo.insert(port).getId();
        //add id to user
        curUser.addToPortfolio(pId);
        //update user by adding current portfolio to user 
        userService.updateUser(curUser);        
    }

    /**
     * FOR TESTING PURPOSES. WANT TO MANUALLY CREATE A PORTFOLIO
     */
    public Portfolio addPortfolio(){
        Portfolio newPortfolio = new Portfolio();
        return repo.insert(newPortfolio);
    }


    // Called when TradeService calls add Trade
    public void addTrade(ObjectId tradeId, ObjectId portfolioId){
        Optional<Portfolio> retrievePortfolio = repo.findById(portfolioId);
        if (!retrievePortfolio.isPresent()){
			log.log(Level.WARNING, "This portfolio does not exist in repo");
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		} else {
            Portfolio portfolio = retrievePortfolio.get();
            log.log(Level.INFO, "Portfolio retrieved with id: " + portfolio.getId());
            log.log(Level.INFO, "Trying to add trade id: " + tradeId);
            portfolio.addTradeIdToOutstanding(tradeId);
            portfolio.addTradeIdToHistory(tradeId);
            repo.save(portfolio);
        }
    }


    public void addStock(String ticker, int quantity, ObjectId portfolioId){
        try{
            Portfolio portfolio = getPortfolioRepo(portfolioId);
            HashMap<String, Stock> portfolio_stocks = portfolio.getStocks();
            if (portfolio_stocks.containsKey(ticker) == false){ //Stock does not exist in portfolio yet
                Stock new_stock = new Stock();
                new_stock.setAmount(quantity);
                new_stock.setTicker(ticker);
                portfolio_stocks.put(ticker, new_stock);
            } else {
                Stock stock = portfolio_stocks.get(ticker);
                stock.setAmount(stock.getAmount() + quantity);
            }
            repo.save(portfolio);
        }
        catch(ResponseStatusException ex){
            log.log(Level.WARNING, "This portfolio does not exist in repo");
        }
    }


    public void removeStock(String ticker, int quantity, ObjectId portfolioId){
        try{
            Portfolio portfolio = getPortfolioRepo(portfolioId);
            HashMap<String, Stock> portfolio_stocks = portfolio.getStocks();
            if (portfolio_stocks.containsKey(ticker) == false){
                log.log(Level.WARNING, "This ticker does not exist in portfolio");
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            Stock stock = portfolio_stocks.get(ticker);
            if (stock.getAmount() < quantity){
                log.log(Level.WARNING, "Insufficient stocks to remove");
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
            } else {
                stock.setAmount(stock.getAmount() - quantity);
                repo.save(portfolio);
            }
        }
        catch(ResponseStatusException ex){
            log.log(Level.WARNING, "This portfolio does not exist in repo");
        }
        
    }
    

    public Portfolio getPortfolioRepo(ObjectId portfolioId){
        Optional<Portfolio> retrievePortfolio = repo.findById(portfolioId);
        if (!retrievePortfolio.isPresent()){
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		} 
        return retrievePortfolio.get();
    }

    
    @Scheduled(fixedRate=10000)
    public void scheduleUpdateOutstandingTrade() {
        log.info("start the interval call to update the outsanding trade for all the portfolios in dbs");
        Trade curTrade;
        List<Portfolio> portList = repo.findAll();
        double expense;
        //update the outstandinList for each portfolio we have
        for(Portfolio p: portList){
            //create a local varible to hold the list 
            List <ObjectId> outListtoRemove = new ArrayList<>();
            for(ObjectId id:p.getOutstandingList()){
                //check the status of outstanding list              
                curTrade = tradeService.getTradeById(id).orElse(null);

                //check if the outstanding is rejected, if yes remove from outstanding list
                if(curTrade != null & curTrade.gettStatus()==TradeStatus.REJECTED){
                    outListtoRemove.add(id);
                }
                   
                //if current trade is not null and the current trade has been fulfilled
                else if(curTrade != null & curTrade.gettStatus()==TradeStatus.FILLED){
                    //remove the trade from the outStandinglist
                    outListtoRemove.add(id);
                    
                    //set the money change
                    expense = curTrade.getQuantity()*curTrade.getRequestPrice();
                    p.setCashOnHand(p.getCashOnHand()-expense);
                    p.setTotalExpense(p.getTotalExpense()+expense);
                
                    //save to repo
                    repo.save(p);

                    //add stock or remove stock according to the trade
                    if(curTrade.getQuantity() > 0) 
                        addStock(curTrade.getStockTicker(), curTrade.getQuantity(), p.getId());
                    else
                        removeStock(curTrade.getStockTicker(), Math.abs(curTrade.getQuantity()), p.getId());
                   
                }

                
            }
            
            p.removeTradesFromOutstanding(outListtoRemove);
            repo.save(p);
        }
        log.info("end of scheduled job");
    }
}