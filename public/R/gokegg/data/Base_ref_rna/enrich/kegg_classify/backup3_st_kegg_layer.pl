#!usr/bin/perl -w

use strict;

die "usage:perl $0 <pathway table> \n" unless @ARGV == 1;

open A,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer1.txt";
open B,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer2.txt";
open C,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer3.txt";
open D,$ARGV[0];

my %lay1;my %lay2;my %lay3;my %tar;my %tar2;my %kk;my %all;my %all2;my %st_gene;my %st_isof;

while(<A>){
    chomp;$lay1{$.}=$_;
    $kk{$_}=$.;
}
close A;

while(<B>){
    chomp;$lay2{$.}=$_;
}
close B;

while(<C>){
    chomp;$lay3{$_}=$.;
}
close C;
my %h;
open OUT,">kegg_func.num";
while(<D>){
    chomp;next if(/^PathWay/);
    my @a=split /\t+/;my @b=split /;/,$a[3];my %h;
    for(my $i=0;$i < @b;$i++){
        my @c=split /[()]/,$b[$i];
        my @d=split /\_se/,$c[0];
        $h{$d[0]}=1;
        $all2{$lay1{$lay3{$a[1]}}}{$d[0]}=1;
        $tar2{$lay1{$lay3{$a[1]}}}{$lay2{$lay3{$a[1]}}}{$d[0]}=1;
        $st_gene{$d[0]}++;$st_isof{$c[0]}++;

        $tar{$lay1{$lay3{$a[1]}}}{$lay2{$lay3{$a[1]}}}{$c[0]}++;
        $all{$lay1{$lay3{$a[1]}}}{$c[0]}++;
    }
    my @site=sort keys %h;my $site=@site;
    print OUT "$site\t$a[2]\t$a[1]\n";
}
close D;close OUT;
my @num_gene=keys %st_gene;my $num_gene=$#num_gene;my @num_isof=keys %st_isof;my $num_isof=@num_isof;
open OUT,">to_figure_classify.txt";
open OUT2,">KEGG_classification.txt";
foreach my $k1(sort {$kk{$a}<=>$kk{$b}} keys %kk){
    if(exists $all{$k1}){
        my @tmp=keys %{$all2{$k1}};my $num=@tmp;
        my @tmpp=keys %{$all{$k1}};my $numm=@tmpp;
        print OUT2 "$k1\t$numm($num)--isoform(gene)\n";
    }
    else{
        print OUT2 "$k1\t0(0)\n";
    }
    foreach my $k2(sort keys %{$tar{$k1}}){
        print OUT2 " --$k2\t";
        if(exists $tar{$k1}{$k2}){
            my @tmp=keys %{$tar2{$k1}{$k2}};my $num=@tmp;
            my @tmpp=keys %{$tar{$k1}{$k2}};my $numm=@tmpp;
            print OUT2 "$numm($num)\n";
            print OUT "$k2";
            printf OUT "\t%d\t%d\t%.4f\t%d\t%.4f\n",$kk{$k1},$numm,$numm/$num_isof,$num,$num/$num_gene;
        }
        else{
            print OUT2 "0(0)\n";
        }
    }
}
close OUT;
`cp /mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_classification_figure.m ./`;
`/mnt/lustre/users/zengl/tools/matlab < kegg_classification_figure.m`;
