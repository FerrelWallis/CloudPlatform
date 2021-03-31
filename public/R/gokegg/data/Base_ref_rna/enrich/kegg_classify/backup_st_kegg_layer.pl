#!usr/bin/perl -w

use strict;

die "usage:perl $0 <path table> \n" unless @ARGV == 1;

open A,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer1.txt";
open B,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer2.txt";
open C,"/mnt/lustre/users/zengl/bin/enrich/kegg_classify/kegg_layer3.txt";
open D,$ARGV[0];


my %lay1;my %lay2;my %lay3;my %tar;my %tar2;
my %kk;my %all;my %all2;

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

while(<D>){
    chomp;next if(/^PathWay/);
    my @a=split /\t+/;
    my @b=split /;/,$a[3];
    for(my $i=0;$i < @b;$i++){
        my @c=split /[()]/,$b[$i];
        $all2{$lay1{$lay3{$a[1]}}}{$c[1]}=1;
        $tar2{$lay1{$lay3{$a[1]}}}{$lay2{$lay3{$a[1]}}}{$c[1]}=1;
    }
    #print "$a[3]\n";
    #print "$lay3{$a[1]}\t$lay1{$lay3{$a[1]}}\n";
    $tar{$lay1{$lay3{$a[1]}}}{$lay2{$lay3{$a[1]}}}+=$a[2];
    $all{$lay1{$lay3{$a[1]}}}+=$a[2];
}
close D;
foreach my $k1(sort {$kk{$a}<=>$kk{$b}} keys %kk){
    if(exists $all{$k1}){
        my @tmp=keys %{$all2{$k1}};my $num=@tmp;
        print "$k1\t$all{$k1}($num)\n";
    }
    else{
        print "$k1\t0(0)\n";
    }
    foreach my $k2(sort keys %{$tar{$k1}}){
        print " --$k2\t";
        if(exists $tar{$k1}{$k2}){
            my @tmp=keys %{$tar2{$k1}{$k2}};my $num=@tmp;
            print "$tar{$k1}{$k2}($num)\n";
        }
        else{
            print "0(0)\n";
        }
    }
}
